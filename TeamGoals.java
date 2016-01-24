package artanis;

import java.util.*;
import battlecode.common.*;

public class TeamGoals {
	
	public TeamGoals ( RobotType type ){
		myType = type;
	}
	
	private RobotType myType;
	private MapLocation myLocation;
	
	// Soldiers keep track of soldiersInDanger and zombieDens
	// they have seen. Archons and scouts keep track of zombieDens,
	// neutralRobots and parts they have seen.
	
	public LinkedList<Signal> soldiersInDanger = new LinkedList<Signal>();
	public LinkedList<MapLocation> zombieDens = new LinkedList<MapLocation>();
	
	public LinkedList<MapLocation> neutralRobots = new LinkedList<MapLocation>();
	public LinkedList<MapLocation> parts = new LinkedList<MapLocation>();
	
	// Maximum size these lists are allowed to have
	private static final int MAX_QUEUE_SIZE = 5;
		
	// A message signal with first coordinate equal to 1001 means
	// "here is a zombie den", and so on
	private static final int HERE_IS_A_DEN = 1001;
	private static final int HERE_IS_A_NEUTRAL = 1002;
	private static final int HERE_ARE_PARTS = 1003;
	private static final int WHERE_ARE_THE_RESOURCES = 1004;
	private static final int TARGET_LOCATION = 1004;
	
	// Someone asked
	private boolean someoneAsked_Dens = false;
	private boolean someoneAsked_Resources = false;
	private MapLocation askerLocation_Dens = null;
	private MapLocation askerLocation_Resources = null;
	
	MapLocation stayClear = null;
	MapLocation targetLocation = null;
	
	// A message signal with first coordinate equal to 1000 means "Let me move!"
	private static final int LET_ME_MOVE = 1000;
	
	private boolean canSeeNewDen = false;
	private boolean canSeeNewParts = false;
	private boolean canSeeNewNeutrals = false;
	
	// Broadcast distances (squared)
	private static final int TINY_RADIUS = 4;
	private static final int SMALLER_RADIUS = 4*RobotType.SOLDIER.attackRadiusSquared;
	private static final int SMALL_RADIUS = 625;
	
	public void update ( Readings readings ) throws GameActionException {
		myLocation = readings.myLocation;
		
		getNewGoalsFrom( readings.signals );
		canSeeNewDen = checkForNewDen( readings.dens );
		canSeeNewParts = checkForNewParts( readings.parts );
		canSeeNewNeutrals = checkForNewNeutrals( readings.neutrals );
		removeCompletedGoals( readings );
	}
	
	public void transmitNewGoal ( RobotController rc ) throws GameActionException {
		if ( canSeeNewDen ) {
			rc.broadcastSignal( SMALL_RADIUS );
			rc.broadcastSignal( SMALL_RADIUS );
		} else if ( canSeeNewParts ) {
			rc.broadcastSignal( SMALL_RADIUS );
			rc.broadcastSignal( SMALL_RADIUS );
			rc.broadcastSignal( SMALL_RADIUS );
		} else if ( canSeeNewNeutrals ) {
			rc.broadcastSignal( SMALL_RADIUS );
			rc.broadcastSignal( SMALL_RADIUS );
			rc.broadcastSignal( SMALL_RADIUS );
			rc.broadcastSignal( SMALL_RADIUS );
		}
	}
	
	public void callForHelp ( RobotController rc ) throws GameActionException {
		rc.broadcastSignal( SMALL_RADIUS );
	}
	
	public void callForHelp ( RobotController rc, int allies ) throws GameActionException {
		rc.broadcastSignal( SMALL_RADIUS + allies*allies );
	}
	
	void askToClearTheWay( RobotController rc ) throws GameActionException {
		rc.broadcastMessageSignal( LET_ME_MOVE, 0, TINY_RADIUS);
	}
	
	void askToClearTheWay( RobotController rc, int radius ) throws GameActionException {
		rc.broadcastMessageSignal( LET_ME_MOVE, 0, TINY_RADIUS + radius*radius);
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
	
private boolean checkForNewParts ( ArrayList<MapLocation> partsLocations ) throws GameActionException {
		
		if ( partsLocations.size() > 0 ) {
			// Check if the given den location is was already knonw
			boolean oldParts = false;
			Iterator<MapLocation> iterator = parts.iterator();
			while( iterator.hasNext() ){
				if( partsLocations.get(0).distanceSquaredTo( iterator.next() ) <= myType.sensorRadiusSquared ) {
					oldParts = true;
					break;
				}
			}
			
			// If not, add it to the set of goals.
			if( !oldParts ) {
				parts.add( partsLocations.get(0) );
				return true;
			}
		}
		
		return false;
	}
	
	private boolean checkForNewNeutrals ( ArrayList<RobotInfo> neutrals ) throws GameActionException {
		
		if ( neutrals.size() > 0 ) {
			// Check if the given den location is was already knonw
			boolean oldDen = false;
			Iterator<MapLocation> iterator = neutralRobots.iterator();
			while( iterator.hasNext() ){
				if( neutrals.get(0).location.distanceSquaredTo( iterator.next() ) <= myType.sensorRadiusSquared ) {
					oldDen = true;
					break;
				}
			}
			
			// If not, add it to the set of goals.
			if( !oldDen ) {
				neutralRobots.add( neutrals.get(0).location );
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
			
			if ( readings.parts.size() == 0 && myType == RobotType.ARCHON ){
				Iterator<MapLocation> iterator3 = parts.iterator();
				while( iterator3.hasNext() ){
					if( myLocation.distanceSquaredTo( iterator3.next() ) <= RobotType.SOLDIER.attackRadiusSquared ) {
						iterator3.remove();
					}
				}
			}
			
			if ( readings.neutrals.size() == 0 && myType == RobotType.ARCHON ){
				Iterator<MapLocation> iterator4 = neutralRobots.iterator();
				while( iterator4.hasNext() ){
					if( myLocation.distanceSquaredTo( iterator4.next() ) <= RobotType.SOLDIER.attackRadiusSquared ) {
						iterator4.remove();
					}
				}
			}
			
		}
	}
	
	public void getNewGoalsFrom ( LinkedList<Signal> signalQueue ) {

		// Separate signals and message signals

		LinkedList<Signal> signals = new LinkedList<Signal>();
		LinkedList<Signal> messageSignals = new LinkedList<Signal>();
		
		for ( Signal beep : signalQueue ) {
			if ( beep.getMessage() == null ) {
				signals.add( beep );
			} else {
				messageSignals.add( beep );
			}
		}
		
		// The number of signals sent by a robot in a turn determines
		// the intended message. The SignalWithId class is essentially
		// the Signal class with different hashCode() and equals( Object other )
		// methods.
		
		Map<SignalWithId,Integer> histogram = new HashMap<SignalWithId, Integer>();
		for (Signal beep : signals) {
			SignalWithId bop = new SignalWithId( beep );
		    Integer value = histogram.get( bop );
		    if (value == null)
		    	histogram.put( bop, 1 );
		    else
		    	histogram.put( bop, value + 1);
		}
		
		for ( SignalWithId beep : histogram.keySet() ) {
			switch ( histogram.get( beep ) ) {
			case 1:
				processCallForHelp( beep.signal );
				break;
			case 2:
				processReply( beep.signal );
				break;
			case 3:
				// Three signals is a den location request.
				// (Robots which can see a zombie den will reply with
				// two signals. See somewhere else in the code.) 
				someoneAsked_Dens = true;
				askerLocation_Dens = beep.signal.getLocation();
				break;
			}
		}
		
		// Now process message Signals
		int message;
		int[] displacement = new int[2];
		for ( Signal beep : messageSignals ) {
			message = beep.getMessage()[0];
			if ( message == LET_ME_MOVE ) {
				stayClear = beep.getLocation();
			} else if ( message == WHERE_ARE_THE_RESOURCES ) {
				someoneAsked_Resources = true;
				askerLocation_Resources = beep.getLocation();
			} else if ( message == TARGET_LOCATION ) {
				displacement = integerToPosition( beep.getMessage()[1] );
				targetLocation = beep.getLocation().add( displacement[0], displacement[1] );
			}
		}
		
	}
	
	// Encodes a pair of integers between -100 and 100 
	// into a single integer.
	public static int positionToInteger ( int[] v ) {
		int a, b;
		a = v[0]+100;
		b = (v[1]+100)*1000;
		return b+a;
	}

	// Decodes an integer into the corresponding pair of integers
	public static int[] integerToPosition ( int n ) {
		int[] v = new int[2];  
		v[0] = (n % 1000);
		v[1] = (n-v[0])/1000;
		v[0] = v[0]-100;
		v[1] = v[1]-100;
		return v;
	}
		
	private void processCallForHelp( Signal beep ) {
		// Erase earlier calls for help from the same soldier
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
	}

	public boolean haveAsked = false;
	
	// Archons ask about the locations of parts and neutral robots.
	// When archons get a reply, they don't know if its the location of
	// parts or of a neutral robot. To be safe it places the location
	// received in the lower priority list of part locations.
	
	// All other units ask about the locations of zombie dens.
	
	private void processReply( Signal beep ) {
		if ( haveAsked ) {
			if ( myType == RobotType.ARCHON ) {
				Iterator<MapLocation> iterator = parts.iterator();
				while( iterator.hasNext() ){
					if( iterator.next().distanceSquaredTo( beep.getLocation() ) <= SMALLER_RADIUS  ) {
						iterator.remove();
					}
				}
				parts.add( beep.getLocation() );
				if( parts.size() > MAX_QUEUE_SIZE ) {
					parts.remove();
				}
			} else {
				Iterator<MapLocation> iterator2 = zombieDens.iterator();
				while( iterator2.hasNext() ){
					if( iterator2.next().distanceSquaredTo( beep.getLocation() ) <= SMALLER_RADIUS  ) {
						iterator2.remove();
					}
				}
				zombieDens.add( beep.getLocation() );
				if( zombieDens.size() > MAX_QUEUE_SIZE ) {
					zombieDens.remove();
				}
			}
		}
	}
	
	/////////////////////////////////////////////////////////////////////	
	//////////// New equals and hashCode methods for Signals ////////////
	/////////////////////////////////////////////////////////////////////
	
	private class SignalWithId {
		SignalWithId( Signal beep ) {
			signal = beep;
			id = beep.getID();
		}
		Signal signal;
		int id;
		
		public boolean equals(Object other) {
		    // Not strictly necessary, but often a good optimization
		    if (this == other)
		      return true;
		    if (!(other instanceof SignalWithId))
		      return false;
		    SignalWithId otherSignalWithId = (SignalWithId) other;
		    
		    if ( this.id == otherSignalWithId.id )
		    	return true;
		    else
		    	return false;
		}
		
		public int hashCode() {
			return this.id;
		}
		
	}

}