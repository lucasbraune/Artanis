package artanis;

import java.util.*;
import battlecode.common.*;

public class Readings {
	
	private RobotController rc = RobotPlayer.rc;
	
	// Robots
	public ArrayList<RobotInfo> enemies = new ArrayList<RobotInfo>();
	public ArrayList<RobotInfo> allies = new ArrayList<RobotInfo>();
	public ArrayList<RobotInfo> enemiesInRange = new ArrayList<RobotInfo>();

	// Zombie dens
	public ArrayList<RobotInfo> dens = new ArrayList<RobotInfo>();
	
	// Resources
	public ArrayList<MapLocation> parts = new ArrayList<MapLocation>();
	public ArrayList<RobotInfo> neutrals = new ArrayList<RobotInfo>();
	
	// Signals
	public LinkedList<Signal> signals = new LinkedList<Signal>();
	
	public void update() {
		// Update robots and zombie dens
		RobotInfo[] robots = rc.senseNearbyRobots();
		
		enemies.clear();
		allies.clear();
		enemiesInRange.clear();
		dens.clear();
		neutrals.clear();
		
		for( int i=0; i<robots.length; i++ ){
			
			if ( robots[i].team == rc.getTeam() ) {
				allies.add( robots[i] );
			} else if ( robots[i].team == rc.getTeam().opponent() ){
				enemies.add( robots[i] );
			} else if ( robots[i].team == Team.ZOMBIE ){
				if ( robots[i].type == RobotType.ZOMBIEDEN ) {
					dens.add( robots[i] );
				} else {
					enemies.add( robots[i] );
				}
			} else if ( robots[i].team == Team.NEUTRAL ) {
				neutrals.add( robots[i] );
			} 
			
		}
		
		for( RobotInfo enemy : enemies ){
			if ( rc.canAttackLocation( enemy.location ) ){
				enemiesInRange.add( enemy );
			}
		}
		
		// Update parts
		parts.clear();
		MapLocation[] locations = rc.sensePartLocations(-1); 
		for ( int i=0; i<locations.length; i++ ){
			parts.add( locations[i] );
		}
		
		// Update signals
		Signal[] buzz = rc.emptySignalQueue();
		signals.clear();
		
		for( int i=0; i<buzz.length; i++ ) {
			if ( buzz[i].getTeam() == rc.getTeam() )
				signals.add( buzz[i] );
		}
	}
	
	public void considerDensAsEnemies() {
		for( RobotInfo den : dens ) {
			enemies.add( den );
			if( rc.canAttackLocation( den.location ) ){
				enemiesInRange.add( den );
			}
		}
	}
	
}
	