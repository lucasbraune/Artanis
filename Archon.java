package artanis;

import battlecode.common.*;

public class Archon {
	
	private static RobotController rc;
	private static Signal[] signalQueue;
	
	// State variables
	private static Direction myDirection;
	private static boolean isMasterArchon = false;
	private static RobotType robotTypeToBeBuilt = RobotType.SCOUT;
	
	// Archon messages start at 1000. Public so that other units can read.
	public static final int I_AM_THE_MASTER = 1000;
	public static final int TARGET_LOCATION = 1001;
	
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

			if( rc.canBuild( randomDir, robotTypeToBeBuilt ) ){
				rc.build(randomDir, robotTypeToBeBuilt);
				if (robotTypeToBeBuilt != RobotType.SOLDIER ) {
					robotTypeToBeBuilt = RobotType.SOLDIER;
				}
			} else {
				signalQueue = rc.emptySignalQueue();
				myDirection = rc.getLocation().directionTo( Communication.getLocation( signalQueue, Archon.TARGET_LOCATION ) );
				Movement.simpleMove( myDirection );
			}
		}
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
		MapLocation aheadOfMe, enemyLocation;
		int round = rc.getRoundNum();
		signalQueue = rc.emptySignalQueue();
		
		enemyLocation = Communication.getLocation( signalQueue, Scout.ENEMY_LOCATION );
		if( enemyLocation != null ){
			myDirection = rc.getLocation().directionTo( enemyLocation );
		}

		if( round % 4 != 1 ) {
			aheadOfMe = Movement.getLocationAhead( myDirection );
			Communication.broadcastLocation( aheadOfMe, TARGET_LOCATION, Communication.SMALL_RADIUS );
		} else {
			if ( !rc.onTheMap( rc.getLocation().add(myDirection, 4) ) ) {
				myDirection = Movement.randomDirection();
				// this changes directions if current one leads out of the map.
			}
			else {
				Movement.simpleMove( myDirection );
			}
		}
	}

}