package artanis;

import java.util.ArrayList;

import battlecode.common.*;

public class Attack {
	
	// This overload is here because I failed to convert an ArrayList into an array elsewhere.
	public static MapLocation findWeakestEnemy( ArrayList<RobotInfo> enemyRobots ){
		RobotInfo[] enemies = new RobotInfo[ enemyRobots.size() ];
		
		for(int i=0; i<enemies.length; i++){
			enemies[i] = enemyRobots.get(i);
		}
		return findWeakestEnemy( enemies );
	}
	
	public static MapLocation findWeakestEnemy( RobotInfo[] enemyRobots ) {
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
	
}