package artanis;

import java.util.Random;

import battlecode.common.*;

public class RobotPlayer {

	private static Random rnd;
	
	private static RobotController rc;
	private static Team myTeam, enemyTeam; 
	
	public static void run(RobotController rcIn){
		
		rc = rcIn;
		myTeam = rc.getTeam();
		enemyTeam = myTeam.opponent();
		
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
		
		if(rc.isWeaponReady()){
			RobotInfo[] enemyRobots = rc.senseHostileRobots(rc.getLocation(), myAttackRadius);
			if(enemyRobots.length>0){
				rc.attackLocation( findWeakestEnemy(enemyRobots) );
			}
			
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