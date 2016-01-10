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
	
	public static void run(RobotController rcIn){
		
		rc = rcIn;
		
		rnd = new Random(rc.getID());
		
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
			}catch(Exception e){
				e.printStackTrace();
			}
			Clock.yield();
		}
	}

	private static void soldierCode() throws GameActionException {
		int myAttackRadius = rc.getType().attackRadiusSquared;

		RobotInfo[] enemyRobots = rc.senseHostileRobots( rc.getLocation(), myAttackRadius );
		if(enemyRobots.length>0 && rc.isWeaponReady()){
				rc.attackLocation( findWeakestEnemy(enemyRobots) );
		} else if ( rc.isCoreReady() ) {
			if (rc.getTeam() == Team.A)
				tryToMove( Direction.SOUTH );
			else
				tryToMove( Direction.NORTH );
		}

	}
	
	private static void tryToMove ( Direction dir ) throws GameActionException {

		// This was taken from Max Mann's tutorials.
		// The idea is to try to move towards dir. If you can't, try to move
		// after changing dir by +1, -1, +2, -2, +3, -3 (meaning that you
		// rotate dir left or right up to three times).
		// Reading the code below bear in mind that 
		// Direction.values() returns an arrway with the the
		// possible directions, i.e., WEST, NORTHWEST, etc. and
		// the method dir.ordinal() returns the index of
		// the direction dir in this array.
		
		Direction candidateDirection = dir;
		
		pastLocations.add( rc.getLocation() );
		if (pastLocations.size() > LOCATIONS_REMEMBERED ) {
			pastLocations.remove(0);
		}
		
		for(int i=0; i<possibleDirections.length; i++ ){
			
			candidateDirection = Direction.values()[ ( dir.ordinal() + possibleDirections[i] + 8 ) % 8 ];
			MapLocation candidateLocation = rc.getLocation().add( candidateDirection );
			
			
			if( rc.canMove(candidateDirection) && !pastLocations.contains( candidateLocation ) ){
				rc.move(candidateDirection);
				increasePatience();
				break;
			} else {
				decreasePatience();
			}
			
			if (patience < 0 ) {
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
		if(rc.isCoreReady()){
			Direction randomDir = randomDirection();
			if(rc.canBuild(randomDir, RobotType.SOLDIER)){
				rc.build(randomDir,RobotType.SOLDIER);
			} 
		}
	}
	

	private static Direction randomDirection() {
		return Direction.values()[(int)(rnd.nextDouble()*8)];
	}
	
}