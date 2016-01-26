package artanis;

import java.util.*;
import battlecode.common.*;

public class Readings {
	
	public Readings( RobotType type, Team team ) {
		myType = type;
		myTeam = team;
	}
	
	private RobotType myType;
	private Team myTeam;
	
	public MapLocation myLocation = null;
	
	// Robots. Enemies = opponents + zombies
	public ArrayList<RobotInfo> enemies = new ArrayList<RobotInfo>();
	public ArrayList<RobotInfo> opponent = new ArrayList<RobotInfo>();
	public ArrayList<RobotInfo> zombies = new ArrayList<RobotInfo>();
	public ArrayList<RobotInfo> allies = new ArrayList<RobotInfo>();
	public ArrayList<RobotInfo> enemiesInRange = new ArrayList<RobotInfo>();
	

	// Zombie dens
	public ArrayList<RobotInfo> dens = new ArrayList<RobotInfo>();
	
	// Resources
	public ArrayList<MapLocation> parts = new ArrayList<MapLocation>();
	public ArrayList<RobotInfo> neutrals = new ArrayList<RobotInfo>();
	
	// Signals
	public LinkedList<Signal> signals = new LinkedList<Signal>();
	
	private static final int SIGNAL_QUEUE_LENGTH = 100; 
	
	public void update( MapLocation myLoc, RobotInfo[] robots, MapLocation[] partLocations, Signal[] signalQueue ) {
		// update myLocation
		myLocation = myLoc;
		
		// Update robots and zombie dens
		enemies.clear();
		opponent.clear();
		zombies.clear();
		allies.clear();
		enemiesInRange.clear();
		dens.clear();
		neutrals.clear();
		
		for( int i=0; i<robots.length; i++ ){
			
			if ( robots[i].team == myTeam ) {
				allies.add( robots[i] );
			} else if ( robots[i].team == myTeam.opponent() ){
				opponent.add( robots[i] );
				enemies.add( robots[i] );
				if( myLocation.distanceSquaredTo( robots[i].location ) <= myType.attackRadiusSquared ) {
					enemiesInRange.add( robots[i] );
				}
			} else if ( robots[i].team == Team.ZOMBIE ){
				if ( robots[i].type == RobotType.ZOMBIEDEN ) {
					dens.add( robots[i] );
				} else {
					enemies.add( robots[i] );
					zombies.add( robots[i] );
					if( myLocation.distanceSquaredTo( robots[i].location ) <= myType.attackRadiusSquared ) {
						enemiesInRange.add( robots[i] );
					}
				}
			} else if ( robots[i].team == Team.NEUTRAL ) {
				neutrals.add( robots[i] );
			} 
			
		}
		
		// Update parts
		parts.clear();
		if( myType != RobotType.ARCHON && partLocations.length>0) {
			parts.add(partLocations[0] );
		} else {
			for ( int i=0; i<partLocations.length; i++ ){
				parts.add( partLocations[i] );			
			}
		}

		
		// Update signals
		signals.clear();
		for( int i=0; i<signalQueue.length && i< SIGNAL_QUEUE_LENGTH; i++ ) {
			if ( signalQueue[i].getTeam() == myTeam )
				signals.add( signalQueue[i] );
		}
	}
	
	public void considerDensAsEnemies() {
		for( RobotInfo den : dens ) {
			enemies.add( den );
			if (  myLocation.distanceSquaredTo( den.location ) <= myType.attackRadiusSquared ) {
				enemiesInRange.add(0, den );
			}
		}
	}
	
}
	