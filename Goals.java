package artanis;

import java.util.*;
import battlecode.common.*;

public class Goals {
	
	public Goals ( RobotType type ){
		myType = type;
	}
	
	private RobotType myType;
	private MapLocation myLocation;
	
	public LinkedList<Signal> archonsInDanger = new LinkedList<Signal>();
	public LinkedList<Signal> soldiersInDanger = new LinkedList<Signal>();
	public LinkedList<MapLocation> zombieDens = new LinkedList<MapLocation>();
	public LinkedList<MapLocation> parts = new LinkedList<MapLocation>();
	
	private boolean canSeeNewDen = false;

	// Maximum size these lists are allowed to have
	private static final int MAX_QUEUE_SIZE = 5;

	// Broadcast distances (squared)
	private static final int SMALL_RADIUS = 625;
	private static final int MEDIUM_RADIUS = 900;
	
	public void update ( Readings readings ) throws GameActionException {
		myLocation = readings.myLocation;
		getNewGoalsFrom( readings.signals );
		canSeeNewDen = checkForNewDen( readings.dens );
		removeCompletedGoals( readings );
	}
	
	public void transmitNewGoal ( RobotController rc ) throws GameActionException {
		if ( canSeeNewDen ) {
			rc.broadcastSignal( SMALL_RADIUS );
			rc.broadcastSignal( SMALL_RADIUS );
		}
	}
	
	public void callForHelp ( RobotController rc ) throws GameActionException {
		rc.broadcastSignal( SMALL_RADIUS );
	}
	
	// Tries to add a den to the goals. Returns true iff succeeds.
	private boolean checkForNewDen ( ArrayList<RobotInfo> dens ) throws GameActionException {
		
		if ( dens.size() > 0 ) {
			// Check if the given den location is was already knonw
			boolean oldDen = false;
			Iterator<MapLocation> iterator = zombieDens.iterator();
			while( iterator.hasNext() ){
				if( dens.get(0).location.distanceSquaredTo( iterator.next() ) <= myType.sensorRadiusSquared ) {
					oldDen = true;
					break;
				}
			}
			
			// If not, add it to the set of goals.
			if( !oldDen ) {
				zombieDens.add( dens.get(0).location );
				return true;
			}
		}
		
		return false;
	}
	

	private void removeCompletedGoals( Readings readings ) {
		
		// If I am in attacking range of the distress signal of a soldier
		// and there are no enemy soldiers nearby, dismiss the distress
		// signal.

		if ( readings.enemies.size() == 0 ) {
			
			Iterator<Signal> iterator = archonsInDanger.iterator();
			while( iterator.hasNext() ){
				if( myLocation.distanceSquaredTo( iterator.next().getLocation() ) <= myType.attackRadiusSquared ) {
					iterator.remove();
				}
			}
			
			Iterator<Signal> iterator1 = soldiersInDanger.iterator();
			while( iterator1.hasNext() ){
				if( myLocation.distanceSquaredTo( iterator1.next().getLocation() ) <= myType.attackRadiusSquared ) {
					iterator1.remove();
				}
			}
			
			if ( readings.dens.size() == 0 ){
				Iterator<MapLocation> iterator2 = zombieDens.iterator();
				while( iterator2.hasNext() ){
					if( myLocation.distanceSquaredTo( iterator2.next() ) <= myType.attackRadiusSquared ) {
						iterator2.remove();
					}
				}
			}
			
		}
	}
	
	public void getNewGoalsFrom ( LinkedList<Signal> signals ) {

		// Start processing signals from soldiers 
		// If signal has no message, it is a signal from a soldier.

		LinkedList<Signal> fromSoldiers = new LinkedList<Signal>();

		for ( Signal beep : signals ) {
			if ( beep.getMessage() == null ) {
				fromSoldiers.add( beep );
			}
		}

		// If that soldier sent only one signal this (last?) turn, it is
		// calling for help. Note that removing an element from a
		// LinkedList return false if the list does not contain the element.

		for ( Signal beep : fromSoldiers ) {

			int count = 0;
			for ( Signal bop : fromSoldiers ) {
				if ( bop.getID() == beep.getID() ) {
					count++;
				}
			}

			switch ( count ) {
			case 1:
				Iterator<Signal> iterator = soldiersInDanger.iterator();
				while( iterator.hasNext() ){
					if( iterator.next().getID() == beep.getID() ) {
						iterator.remove();
					}
				}
				soldiersInDanger.add( beep );
				if( soldiersInDanger.size() > MAX_QUEUE_SIZE ) {
					soldiersInDanger.remove();
				}
				break;
			case 2:
				Iterator<MapLocation> iterator2 = zombieDens.iterator();
				while( iterator2.hasNext() ){
					if( iterator2.next().distanceSquaredTo( beep.getLocation() ) < myType.attackRadiusSquared ) {
						iterator2.remove();
					}
				}
				zombieDens.add( beep.getLocation() );
				if( zombieDens.size() > MAX_QUEUE_SIZE ) {
					zombieDens.remove();
				}
				break;
//			case 3:
//				Iterator<MapLocation> iterator3 = parts.iterator();
//				while( iterator3.hasNext() ){
//					if( iterator3.next().distanceSquaredTo( beep.getLocation() ) < myType.attackRadiusSquared ) {
//						iterator3.remove();
//					}
//				}
//				parts.add( beep.getLocation() );
//				if( parts.size() > MAX_QUEUE_SIZE ) {
//					parts.remove();
//				}
//				break;
			}
		}

//		// Start processing message signals
//
//		for ( Signal ding : signals ) {
//			if ( ding.getMessage() != null && ding.getMessage().length > 0 ) {
//				int message = ding.getMessage()[0]; 
//				
//				switch ( message ) {
//				case Archon.HELP:
//					Iterator<Signal> iterator3 = archonsInDanger.iterator();
//					while( iterator3.hasNext() ){
//						if( iterator3.next().getID() == ding.getID() ) {
//							iterator3.remove();
//						}
//					}
//					archonsInDanger.add( ding );
//					if( archonsInDanger.size() > MAX_QUEUE_SIZE ) {
//						archonsInDanger.remove();
//					}
//					break;
//				}
//				
//			}
//		}
	}

}