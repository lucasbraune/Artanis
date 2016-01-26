package artanis;

import java.util.*;
import battlecode.common.*;

public class Viper extends Soldier {

	Viper( RobotController rcIn ) {
		super(rcIn);
	}
	
	// We save the id's of enemies that have been infected and keep
	// track of the time they stay infected using Timers.
	
	Hashtable<Integer,Timer> infectionTimers = new Hashtable<Integer,Timer>();

	void chooseAndAttackAnEnemy() throws GameActionException {
		
		// We remove the from the list of ids and timers those that
		// that correspond to robots which are no longer infected.

		Iterator<Integer> iterator = infectionTimers.keySet().iterator();
		while( iterator.hasNext() ){
			if( ! infectionTimers.get( iterator.next() ).isWaiting() ) {
				iterator.remove();
			}
		}
		
		// Here we list the candidate robots to be shot at.
		// This method is currently only being used when there are
		// enemies in range, so the if condition is always satisfied.
		
		ArrayList<RobotInfo> candidates = new ArrayList<RobotInfo>(); 

		if ( readings.enemiesInRange.size() > 0 ){
			for ( int i=0; i<readings.enemiesInRange.size(); i++ ) {
				candidates.add( readings.enemiesInRange.get(i) );
			}
		} else {
			for ( int i=0; i<readings.enemies.size(); i++ ) {
				candidates.add( readings.enemies.get(i) );
			}
		}
		
		// Here we create a sublist with those candidates which belong to the
		// other team and are not infected.
		
		Team myOpponent = rc.getTeam().opponent();
		ArrayList<RobotInfo> preferredCandidates = new ArrayList<RobotInfo>();
		
		for( RobotInfo robot : candidates ) {
			if ( robot.team == myOpponent && ! infectionTimers.containsKey( robot.ID ) ) {
				preferredCandidates.add( robot );
			}
		}
		
		if ( preferredCandidates.size() > 0 ) {
			candidates = preferredCandidates;
			rc.setIndicatorString( 2 , "Shooting preferred candidates.");
		} else {
			rc.setIndicatorString( 2 , "All I have is normal candidates.");
		}
		
		RobotInfo[] candidatesArray = candidates.toArray( new RobotInfo[ candidates.size() ] );
		if ( candidatesArray!=null && candidatesArray.length > 0 ) {
			rc.setIndicatorString( 1 , "There are candidate weakest enemies.");
		} else {
			rc.setIndicatorString( 1 , "No candidate weakest enemy.");
		}
		
		RobotInfo target = findWeakestRobot( candidatesArray );
		rc.attackLocation( target.location );
		
		if ( infectionTimers.containsKey( target.ID ) ) {
			infectionTimers.get( target.ID ).reset();
		} else if ( target.team == myOpponent ) {
			infectionTimers.put( target.ID, new Timer( VIPER_INFECTION_LENGTH ) );
		}
	}
	
	private static final int VIPER_INFECTION_LENGTH = 20;
	
}