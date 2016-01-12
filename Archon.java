package artanis;

import battlecode.common.*;

public class Archon {
	
	private static RobotController rc;
	private static Signal[] signalQueue;
	private static Direction myDirection;
	private static boolean isMasterArchon = false;
	private static MapLocation enemyLocation;
	// The following two are used in method locateEnemy()
	private static boolean askedScouts = false; 
	private static int wait=0;
	// Normal archon variables
	private static RobotType typeToBeBuilt = RobotType.SOLDIER;
	
	// Archon messages start at 1000. Public so that other units can read.
	public static final int I_AM_THE_MASTER = 1000;
	public static final int TARGET_LOCATION = 1001;
	public static final int BUILD_TYPE = 1002;
	public static final int WHERE_IS_THE_ENEMY = 1003;
	
	public static void code( ){
		rc = RobotPlayer.rc;
		myDirection = Movement.randomDirection();
		
		try {
			runsOnce();
		} catch ( Exception e ) {
			e.printStackTrace();
		}
		while(true){
			try{
				repeat();
			}catch ( Exception e ) {
				e.printStackTrace();
			}
			Clock.yield();
		}	
	}
	
	private static void runsOnce() throws GameActionException {
		electMasterArchon();
	}

	private static void repeat() throws GameActionException {
		if ( isMasterArchon == true ) {
			masterArchonCode();
		} else {
			normalArchonCode();
		}
	}

	private static void normalArchonCode() throws GameActionException {
		if(rc.isCoreReady()){
			Direction randomDir = Movement.randomDirection();
			int typeNumber;
			MapLocation targetLocation;
			MapLocation[] partsLocations;

			signalQueue = rc.emptySignalQueue();
			targetLocation = Communication.getLocation( signalQueue, Archon.TARGET_LOCATION );
			typeNumber = getBuildRequest( signalQueue );
			if( typeNumber >= 0 ) {
				typeToBeBuilt = RobotType.values()[ typeNumber ];
			} 

			if( targetLocation != null ) {
				if( rc.getLocation().distanceSquaredTo( targetLocation ) < rc.getType().attackRadiusSquared ){
					if( rc.canBuild( randomDir, typeToBeBuilt ) ){
						rc.build(randomDir, typeToBeBuilt);
						if (typeToBeBuilt != RobotType.SOLDIER ) {
							typeToBeBuilt = RobotType.SOLDIER;
						}
					} else {
						partsLocations = rc.sensePartLocations(-1);
						if( partsLocations.length > 0 ) {
							myDirection = rc.getLocation().directionTo( Movement.findClosest( partsLocations ) );
						} else {
							myDirection = rc.getLocation().directionTo( targetLocation );
						}
						Movement.simpleMove( myDirection );
					}
				} else {
					myDirection = rc.getLocation().directionTo( targetLocation );
					Movement.simpleMove( myDirection );
				}
			} else {
				if( rc.canBuild( randomDir, typeToBeBuilt ) ){
					rc.build(randomDir, typeToBeBuilt);
					if (typeToBeBuilt != RobotType.SOLDIER ) {
						typeToBeBuilt = RobotType.SOLDIER;
					}
				} else {
					partsLocations = rc.sensePartLocations(-1);
					if( partsLocations.length > 0 ) {
						myDirection = rc.getLocation().directionTo( Movement.findClosest( partsLocations ) );
					} else {
						myDirection = Movement.randomDirection();
					}
					Movement.simpleMove( myDirection );
				}
			}
		}
	}

	private static int getBuildRequest( Signal[] signals ) {
		Signal signal = Communication.getSignal( signals, BUILD_TYPE );
		if( signal != null && signal.getMessage().length > 0 ){
			return signal.getMessage()[0];
		} else {
			return -1;
		}
		
	}
	
	// Method so I can forget the build request implementation
	private static void sendBuildRequest( RobotType type ) throws GameActionException{
		rc.broadcastMessageSignal( type.ordinal(), BUILD_TYPE, Communication.SMALL_RADIUS );
	}

	private static void electMasterArchon() throws GameActionException {
		Signal latestSignal = Communication.getSignal( rc.emptySignalQueue(), I_AM_THE_MASTER ); 
				
		if( latestSignal == null ) {
			isMasterArchon = true;
			rc.broadcastMessageSignal(0, I_AM_THE_MASTER, Communication.LARGE_RADIUS );
			rc.setIndicatorString(0, "Hello. I am the MASTER ARCHON.");
		} else {
			rc.setIndicatorString(0, "Hello. I am the an Archon.");
		}
	}
	
	private static void masterArchonCode() throws GameActionException {
		MapLocation aheadOfMe;
		int round = rc.getRoundNum();
		signalQueue = rc.emptySignalQueue();
		
		locateEnemy();
		
		if( enemyLocation != null ){
			myDirection = rc.getLocation().directionTo( enemyLocation );
		}

		if( round % 3 != 1 ) {
			aheadOfMe = Movement.getLocationAhead( myDirection );
			Communication.broadcastLocation( aheadOfMe, TARGET_LOCATION, Communication.SMALL_RADIUS );
		} else {
			if ( !rc.onTheMap( rc.getLocation().add(myDirection, 4) ) ) {
				myDirection = Movement.randomDirection();
				rc.setIndicatorString(1, "I don't have the enemy location." );
				// this changes directions if current one leads out of the map.
			}
			else {
				Movement.simpleMove( myDirection );
			}
		}
	}
	
	private static void locateEnemy() throws GameActionException {
		int mySensorRange = rc.getType().sensorRadiusSquared;
		RobotInfo[] enemyRobots = rc.senseNearbyRobots( mySensorRange, rc.getTeam().opponent() );
		
		// If there are robots nearby, I know the enemy location. The wait=0 and askedScouts=false
		// lines interrupt any locateEnemy() search.
		if( enemyRobots.length > 2 ){
			enemyLocation = enemyRobots[0].location;
			wait=0;
			askedScouts = false;
			rc.setIndicatorString(1, "I see the enemy.");
			return;
		}
		// If I am walking to the enemy location and I have not reached it yet, I know where it is.
		// But if I have reached it and see no enemies around (since the if clause above is
		// necessarily false at this point), I should start looking for enemies again.
		if( enemyLocation != null &&
				rc.getLocation().distanceSquaredTo( enemyLocation ) > mySensorRange/2 ) {
			wait=0;
			askedScouts = false;
			rc.setIndicatorString(1, "I am going to the enemy's location.");
			return;
		} 
		// In looking for enemies there will be waiting periods. If during one of these I receive
		// the enemy location, I use it and interrupt the search for enemies.
		if( wait>0 ){
			wait--;
			enemyLocation = Communication.getLocation( signalQueue, Scout.ENEMY_LOCATION );
			if( enemyLocation != null ){
				wait = 0;
				askedScouts = false;
			}
			return;
		}
		// Search goes: Ask scouts where the enemy is. Wait for an answer. If none comes, 
		// tell normal archons to build scouts.
		if( !askedScouts ){
			rc.broadcastMessageSignal(0, WHERE_IS_THE_ENEMY, Communication.LARGE_RADIUS);
			askedScouts = true;
			wait = 50;
			rc.setIndicatorString(1, "I asked scouts the enemy location." );
			return;
		}
		// At this point no scout replied
		if( askedScouts ){
			sendBuildRequest( RobotType.SCOUT );
			askedScouts = false;
			wait = 50;
			rc.setIndicatorString(1, "No scouts replied. I ordered some more from Amazon.");
		}
	}
}