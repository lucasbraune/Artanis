package artanis;

import java.util.ArrayList;
import java.util.Random;

import battlecode.common.*;

public class RobotPlayer {

	private static Random rnd;
	
	private static RobotController rc; 
	private static int[] possibleDirections = {0,1,-1,2,-2,3,-3};
	private static ArrayList<MapLocation> pastLocations = new ArrayList<MapLocation>();
	private static final int LOCATIONS_REMEMBERED = 10;
	
	private static final int MAX_PATIENCE = 10;
	private static final int MIN_PATIENCE = -10;
	private static final int PATIENCE_DECREASE = 3;
	private static final int PATIENCE_INCREASE = 1;
	private static int patience = MAX_PATIENCE; // Start clearing rubble if this drops below zero;
	
	private static Team myTeam;

	private static boolean masterArchon = false; // Identifies the leading Archon.
	private static Direction groupDirection;
	
	public static void run(RobotController rcIn){
		
		rc = rcIn;
		myTeam = rc.getTeam();
		rnd = new Random(rc.getID());
		groupDirection = randomDirection();
		
		try {
			if( rc.getType() == RobotType.ARCHON )
				electMasterArchon();
		} catch ( Exception e ) {
			e.printStackTrace();
		}
		
		
		while(true){
			try{
				if(rc.getType()==RobotType.ARCHON){
					archonCode();
				}else if(rc.getType()==RobotType.TURRET){
					turretCode();
				}else if(rc.getType()==RobotType.TTM){
					ttmCode();
				}else if(rc.getType()==RobotType.GUARD){
					guardCode();
				}else if(rc.getType()==RobotType.SCOUT){
					scoutCode();
				}else if(rc.getType()==RobotType.SOLDIER){
					soldierCode();
				}
			}catch ( Exception e ) {
				e.printStackTrace();
			}
			Clock.yield();
		}
	}

	private static void electMasterArchon() throws GameActionException {
		Signal[] signals = rc.emptySignalQueue();
		int signalsReceived = 0;
		
		rc.setIndicatorString(0, "Hello. I am an Archon.");
		
		for (int i=0; i<signals.length; i++) {
			if ( signals[i].getTeam() == myTeam ) {
				signalsReceived++;
			}
		}
		
		if( signalsReceived == 0 ) {
			masterArchon = true;
			rc.setIndicatorString(0, "Hello. I am the MASTER ARCHON.");
		}
		
		rc.broadcastMessageSignal(rc.getID(), 0, 10000);
	}

	private static void soldierCode() throws GameActionException {
		int myAttackRadius = rc.getType().attackRadiusSquared;

		RobotInfo[] enemyRobots = rc.senseHostileRobots( rc.getLocation(), myAttackRadius );
		if(enemyRobots.length>0 && rc.isWeaponReady()){
				rc.attackLocation( findWeakestEnemy(enemyRobots) );
		} else {
			tryToMove( getDirections() );
		}

	}
	
	private static void tryToMove ( Direction dir ) throws GameActionException {

		// This was taken from Max Mann's tutorials.
		
		Direction candidateDirection = dir;
		boolean coreReady = rc.isCoreReady();
		
		if ( coreReady ) {
			
			pastLocations.add( rc.getLocation() );
			if (pastLocations.size() > LOCATIONS_REMEMBERED ) {
				pastLocations.remove(0);
			}
			
			for(int i=0; i<possibleDirections.length; i++ ){
				
				// The idea is to try to move towards dir. If you can't, try to move
				// after changing dir by +1, -1, +2, -2, +3, -3 (meaning that you
				// rotate dir left or right up to three times).
				// Reading the code below bear in mind that 
				// Direction.values() returns an arrway with the the
				// possible directions, i.e., WEST, NORTHWEST, etc. and
				// the method dir.ordinal() returns the index of
				// the direction dir in this array.
				
				candidateDirection = Direction.values()[ ( dir.ordinal() + possibleDirections[i] + 8 ) % 8 ];
				MapLocation candidateLocation = rc.getLocation().add( candidateDirection );
				
				if( rc.canMove(candidateDirection) && !pastLocations.contains( candidateLocation ) ){
					rc.move(candidateDirection);
					coreReady = false;
					increasePatience();
					break;
				} else {
					decreasePatience();
				}
				
			}
			if (patience < 0 && coreReady ) {
				if ( rc.senseRubble( rc.getLocation().add( dir ) ) > GameConstants.RUBBLE_OBSTRUCTION_THRESH ) {
					rc.clearRubble( dir );
					patience += PATIENCE_INCREASE;
				}
			}
		
		}
	}
	
	private static void increasePatience() {
		if ( patience <= MAX_PATIENCE - PATIENCE_INCREASE) {
			patience += PATIENCE_INCREASE;
		} else {
			patience = MAX_PATIENCE;
		}
	}
	
	private static void decreasePatience() {
		patience -= PATIENCE_DECREASE;
		if (patience <= MIN_PATIENCE) {
			patience = MIN_PATIENCE;
		}
	}
	
	private static MapLocation findWeakestEnemy( RobotInfo[] enemyRobots ) {
		int numberOfEnemies = enemyRobots.length;
		
		if( numberOfEnemies > 0 ){
			int i, weakestIndex = 0;
			double weakness = 0, largestWeakness;
	
			largestWeakness = enemyRobots[0].maxHealth - enemyRobots[0].health;
	
			for(i=1; i<numberOfEnemies; i++) {
				weakness = enemyRobots[i].maxHealth - enemyRobots[i].health;
				if ( weakness > largestWeakness ){
					weakestIndex = i;
					largestWeakness = weakness;
				}
			}
			
			return enemyRobots[weakestIndex].location;
			
		} else {
			return null;
		}
	}

	private static void scoutCode() {
		// TODO Auto-generated method stub
		
	}

	private static void guardCode() {
		// TODO Auto-generated method stub
		
	}

	private static void ttmCode() {
		// TODO Auto-generated method stub
		
	}

	private static void turretCode() {
		// TODO Auto-generated method stub
		
	}

	private static void archonCode() throws GameActionException {
		int round = rc.getRoundNum();
		
		if ( masterArchon == true ) {
			// Master archon code:
			
			// Master archon movement = movement of the whole group
			if( rc.getRoundNum() % 4 != 1 ) {
				rc.broadcastMessageSignal( groupDirection.ordinal() , 0, 1000 );
			} else {
				if ( !rc.onTheMap( rc.getLocation().add(groupDirection, 4) ) ) {
					groupDirection = randomDirection();
				}
				else {
					tryToMove( groupDirection );
				}
			}
		} else {
			// Normal archon code:
			
			// The following has some bytecode inefficiency because archons
			// end up calling rc.isCoreReady() twice before moving. 
			if(rc.isCoreReady()){
				Direction randomDir = randomDirection();
				if(rc.canBuild(randomDir, RobotType.SOLDIER)){
					rc.build(randomDir, RobotType.SOLDIER);
				} else {
					groupDirection = getDirections();
					tryToMove( groupDirection );
				}
			}
		}
		
	}
	

	private static Direction getDirections() {
		
		Direction newGroupDirection = groupDirection, MADirection = groupDirection;
		MapLocation inFrontOfMA = rc.getLocation();
		Signal[] signals = rc.emptySignalQueue();
		Signal latestSignal = null;
		
		for( int i=signals.length-1; i >= 0; i-- ) {
			if ( signals[i].getTeam() == myTeam  ) {
				latestSignal = signals[i];
				break;
			}
		}
		
		if ( latestSignal != null ) {
			MADirection = Direction.values()[ latestSignal.getMessage()[0] ];
			inFrontOfMA = latestSignal.getLocation().add( MADirection , 3 );
			newGroupDirection = rc.getLocation().directionTo( inFrontOfMA );
		}
		
		return newGroupDirection;
	}

	private static Direction randomDirection() {
		return Direction.values()[(int)(rnd.nextDouble()*8)];
	}
	
}