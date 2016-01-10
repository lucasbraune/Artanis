package artanis;

import battlecode.common.*;

public class Attack {
	
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