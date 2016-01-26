package artanis;

import java.util.*;
import battlecode.common.*;

public class TeamGoals {
	
	public TeamGoals ( RobotType type ){
		myType = type;
	}
	
	private RobotType myType;
	private MapLocation myLocation;
	
	// Known locations
	public LinkedList<Signal> soldiersInDanger = new LinkedList<Signal>();
	public LinkedList<MapLocation> zombieDens = new LinkedList<MapLocation>();
	public LinkedList<MapLocation> neutralRobots = new LinkedList<MapLocation>();
	public LinkedList<MapLocation> parts = new LinkedList<MapLocation>();
	
	// Maximum size these lists are allowed to have
	private static final int MAX_QUEUE_SIZE = 5;
	
	// Orders
	MapLocation stayClear = null;
	MapLocation targetLocation = null;
	boolean avoidingOpponent = true;
		
	// Message Meanings
	static final int LET_ME_MOVE = 1000;
	public static final int HERE_IS_A_DEN = 1001;
	static final int HERE_IS_A_NEUTRAL = 1002;
	static final int HERE_ARE_PARTS = 1003;
	static final int WHERE_ARE_THE_RESOURCES = 1004;
	static final int TARGET_LOCATION = 1005;
	public static final int WHERE_IS_THE_TARGET = 1006;
	public static final int SCOUT_ALIVE = 1007;
	
	// Broadcast distances (squared)
	static final int TINY_RADIUS = 4;
	static final int SMALLER_RADIUS = 4*RobotType.SOLDIER.sensorRadiusSquared;
	static final int SMALL_RADIUS = 225;
	static final int MEDIUM_RADIUS = 1500;
	
	// Used to wait before asking the location of a den again
	private static final int RESPONSE_WAITING_PERIOD = 3;
	Timer responseTimer = new Timer( RESPONSE_WAITING_PERIOD );
	
	
	// Timer to count time since last heard from a scout. If this reaches zero,
	// more scouts should be built.
	private static final int SCOUT_CHECK_IN_PERIOD = 100;
	Timer scoutCheckInTimer = new Timer( SCOUT_CHECK_IN_PERIOD );
	public boolean weHaveScouts =  false;
	
	public void update ( Readings readings ) throws GameActionException {
		myLocation = readings.myLocation;
		if ( !responseTimer.isWaiting() ) {
			haveAsked = false;
			responseTimer.reset();
		}
		stayClear = null;
		
		if( !scoutCheckInTimer.isWaiting() ) {
			weHaveScouts = false;
		}
		
		goalsFromSignals( readings.signals );
		
		if( timeSinceLastHeardTargetLocation > TARGET_LOCATION_MEMORY ) {
			targetLocation = null;
		}
		
		goalsFromDenReadings( readings.dens );
		goalsFromPartReadings( readings.parts );
		goalsFromNeutralReadings( readings.neutrals );
		
		removeCompletedGoals( readings );
	}
	
	// Someone asked
	private boolean someoneAsked_Dens = false;
	private boolean someoneAsked_Resources = false;
	private MapLocation askerLocation_Dens = null;
	private MapLocation askerLocation_Resources = null;
	
	// Used to wait before answering a request again
	private static final int ANSWERING_PERIOD = 1;
	private Timer answeringTimer = new Timer( ANSWERING_PERIOD );
	
	void replyWithDensAndResources( RobotController rc, Readings readings ) throws GameActionException {
		if ( !answeringTimer.isWaiting() ) {
			if ( myType == RobotType.ARCHON || myType == RobotType.SCOUT ) {
				if ( someoneAsked_Dens && !zombieDens.isEmpty() ) {
					broadcastLocation( rc, zombieDens.getLast(), HERE_IS_A_DEN, MEDIUM_RADIUS );
					answeringTimer.reset();
					rc.setIndicatorString(2, "Broadcasting den location");
				}
				if ( someoneAsked_Resources ) {
					if ( !parts.isEmpty() ) {
						broadcastLocation( rc, parts.getLast(), HERE_ARE_PARTS, MEDIUM_RADIUS );
						answeringTimer.reset();
						rc.setIndicatorString(2, "Broadcasting parts location");
					}
					if ( !neutralRobots.isEmpty() ) { 
						broadcastLocation( rc, neutralRobots.getLast(), HERE_IS_A_NEUTRAL, MEDIUM_RADIUS );
						answeringTimer.reset();
						rc.setIndicatorString(2, "Broadcasting neutral robot location");
					}
				}
			} else {
				if ( someoneAsked_Dens && readings.dens.size() > 0 ) {
					rc.broadcastSignal( SMALL_RADIUS );
					rc.broadcastSignal( SMALL_RADIUS );
					answeringTimer.reset();
					rc.setIndicatorString(2, "Broadcasting den location");
				} else if (someoneAsked_Resources && ( readings.neutrals.size()>0 || readings.parts.size()>0 ) ) {
					rc.broadcastSignal( MEDIUM_RADIUS );
					rc.broadcastSignal( MEDIUM_RADIUS );
					answeringTimer.reset();
					rc.setIndicatorString(2, "Broadcasting resource location");
				}
			}
			someoneAsked_Dens = false;
			someoneAsked_Resources =false;
		}
	}


	public void callForHelp ( RobotController rc, int allies ) throws GameActionException {
		if( !haveAsked ) {
			if( avoidingOpponent ) {
				rc.broadcastSignal( SMALL_RADIUS + allies*allies);
				rc.setIndicatorString(2, "Calling for help in a small radius.");
			} else {
				rc.broadcastSignal( SMALL_RADIUS + allies*allies );
				rc.setIndicatorString(2, "Calling for help in a small radius.");
			}
		}
	}
	
	public void askForDenLocation ( RobotController rc ) throws GameActionException {
		rc.broadcastSignal( SMALL_RADIUS );
		rc.broadcastSignal( SMALL_RADIUS );
		rc.broadcastSignal( SMALL_RADIUS );
		haveAsked = true;
		rc.setIndicatorString(2, "Asking for den location");
	}
	
	public void askForResourceLocations ( RobotController rc ) throws GameActionException {
		if ( myType == RobotType.ARCHON || myType == RobotType.SCOUT ) {
			rc.broadcastMessageSignal( WHERE_ARE_THE_RESOURCES, 0, MEDIUM_RADIUS );
			rc.setIndicatorString(2, "Asking resource location");
			haveAsked = true;
		}
	}
	
	public void askForTargetLocation ( RobotController rc ) throws GameActionException {
		if ( myType == RobotType.ARCHON || myType == RobotType.SCOUT ) {
			rc.broadcastMessageSignal( WHERE_IS_THE_TARGET, 0, MEDIUM_RADIUS );
			rc.setIndicatorString(2, "Asking target location");
			haveAsked = true;
		}
	}

	void askToClearTheWay( RobotController rc, int patience ) throws GameActionException {
		if ( myType == RobotType.ARCHON || myType == RobotType.SCOUT ) {
			rc.broadcastMessageSignal( LET_ME_MOVE, 0, TINY_RADIUS + patience*patience );
			rc.setIndicatorString(2, "Make way for me!");
		}
	}

	//////////////////////////////////////////////////////////////////////////
	///////////// Get goals form dens, parts and neutrals nearby /////////////
	//////////////////////////////////////////////////////////////////////////
	
	// Tries to add a den to the goals. Returns true iff succeeds.
	private boolean goalsFromDenReadings ( ArrayList<RobotInfo> dens ) throws GameActionException {

		if ( dens.size() > 0 ) {
			// Check if the given den location is was already known
			boolean oldDen = false;
			Iterator<MapLocation> iterator = zombieDens.iterator();
			while( iterator.hasNext() ){
				if( dens.get(0).location.distanceSquaredTo( iterator.next() ) <= SMALLER_RADIUS ) {
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

	private boolean goalsFromPartReadings ( ArrayList<MapLocation> partsLocations ) throws GameActionException {

		if ( partsLocations.size() > 0 ) {
			// Check if the given den location is was already knonw
			boolean oldParts = false;
			Iterator<MapLocation> iterator = parts.iterator();
			while( iterator.hasNext() ){
				if( partsLocations.get(0).distanceSquaredTo( iterator.next() ) <= SMALLER_RADIUS ) {
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
	
	private boolean goalsFromNeutralReadings ( ArrayList<RobotInfo> neutrals ) throws GameActionException {
		
		if ( neutrals.size() > 0 ) {
			// Check if the given den location is was already knonw
			boolean oldDen = false;
			Iterator<MapLocation> iterator = neutralRobots.iterator();
			while( iterator.hasNext() ){
				if( neutrals.get(0).location.distanceSquaredTo( iterator.next() ) <= SMALLER_RADIUS ) {
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

	/////////////////////////////////////////////////////////////////////////////
	//////// Method that checks if goals were completed and removes them //////// 
	/////////////////////////////////////////////////////////////////////////////
	
	
	private void removeCompletedGoals( Readings readings ) {
		
		// If I am in attacking range of the distress signal of a soldier
		// and there are no enemy soldiers nearby, dismiss the distress
		// signal.

		if ( readings.enemies.size() == 0 ) {
			
			Iterator<Signal> iterator1 = soldiersInDanger.iterator();
			while( iterator1.hasNext() ){
				if( myLocation.distanceSquaredTo( iterator1.next().getLocation() ) <= RobotType.SOLDIER.attackRadiusSquared ) {
					iterator1.remove();
				}
			}
			
			if ( readings.dens.size() == 0 ){
				Iterator<MapLocation> iterator2 = zombieDens.iterator();
				while( iterator2.hasNext() ){
					if( myLocation.distanceSquaredTo( iterator2.next() ) <= RobotType.SOLDIER.attackRadiusSquared ) {
						iterator2.remove();
					}
				}
			}
			
			if ( readings.parts.size() == 0 ){
				Iterator<MapLocation> iterator3 = parts.iterator();
				while( iterator3.hasNext() ){
					if( myLocation.distanceSquaredTo( iterator3.next() ) <= RobotType.SOLDIER.attackRadiusSquared ) {
						iterator3.remove();
					}
				}
			}
			
			if ( readings.neutrals.size() == 0 ){
				Iterator<MapLocation> iterator4 = neutralRobots.iterator();
				while( iterator4.hasNext() ){
					if( myLocation.distanceSquaredTo( iterator4.next() ) <= RobotType.SOLDIER.attackRadiusSquared ) {
						iterator4.remove();
					}
				}
			}
			
		}
	}
	
	////////////////////////////////////////////////////////////////////////
	//////////////////////// Get goals from signals ////////////////////////
	////////////////////////////////////////////////////////////////////////
	
	public void goalsFromSignals ( LinkedList<Signal> signalQueue ) {

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
				processSignalReply( beep.signal );
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
		
		timeSinceLastHeardTargetLocation++;
		
		for ( Signal beep : messageSignals ) {
			message = beep.getMessage()[0];
			if ( message == LET_ME_MOVE ) {
				stayClear = beep.getLocation();
				RobotPlayer.rc.setIndicatorString(1, "Received a move away request.");
			} else if ( message == WHERE_ARE_THE_RESOURCES ) {
				someoneAsked_Resources = true;
				askerLocation_Resources = beep.getLocation();
				RobotPlayer.rc.setIndicatorString(1, "Received resource location request.");
			} else if ( message == TARGET_LOCATION ) {
				targetLocation = messageSignalToLocation( beep );
				timeSinceLastHeardTargetLocation = 0;
				RobotPlayer.rc.setIndicatorString(1, "Received target location.");
			} else if ( message == SCOUT_ALIVE ) {
				weHaveScouts = true;
				scoutCheckInTimer.reset();
				RobotPlayer.rc.setIndicatorString(1, "Received a scout check in.");
			} else {
				processMessageSignalReply( beep );
			}
		}
		
	}
	
	private static final int TARGET_LOCATION_MEMORY = 30;
	private int timeSinceLastHeardTargetLocation = 10000;
	
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
	
	// Archons ask about the locations of parts and neutral robots.
	// When archons get a reply, they don't know if its the location of
	// parts or of a neutral robot. To be safe it places the location
	// received in the lower priority list of part locations.
	// All other units ask about the locations of zombie dens.
	// The haveAsked field is important for when a soldier receives a
	// reply it only adds to the list of zombie den locations
	// if it has asked the question. Because otherwise the signal
	// is likely giving an archon the location of parts, which we
	// don't want in the zombie dens list.
	
	public boolean haveAsked = false;
	
	private void processSignalReply( Signal beep ) {
		MapLocation signalOrigin = beep.getLocation();
		if ( haveAsked ) {
			if ( myType == RobotType.ARCHON ) {
				addLocation( signalOrigin, parts );
				RobotPlayer.rc.setIndicatorString(2, "Resource location received");
			} else {
				addLocation( signalOrigin, zombieDens );
				RobotPlayer.rc.setIndicatorString(2, "Den location received");
			}
		}
		// If someone asked for dens or resource locations
		// and they also got this signal, there is no reason for
		// me to reply to them.
		if ( someoneAsked_Dens && askerLocation_Dens.distanceSquaredTo( signalOrigin ) <= SMALL_RADIUS )
			someoneAsked_Dens = false;
		if ( someoneAsked_Resources && askerLocation_Resources.distanceSquaredTo( signalOrigin ) <= MEDIUM_RADIUS )
			someoneAsked_Resources = false;
	}
	
	private void processMessageSignalReply( Signal beep ){
		int[] message = beep.getMessage();
		if ( message != null && message.length >= 2 ) {
			if ( message[0] == HERE_ARE_PARTS ) {
				addLocation( messageSignalToLocation( beep ), parts );
				RobotPlayer.rc.setIndicatorString(2, "Parts location received");
			} else if ( message[0] == HERE_IS_A_NEUTRAL ){
				addLocation( messageSignalToLocation( beep ), neutralRobots );
				RobotPlayer.rc.setIndicatorString(2, "Neutral Robot location received");
			} else if ( message[0] == HERE_IS_A_DEN ){
				addLocation( messageSignalToLocation( beep ), zombieDens );
				RobotPlayer.rc.setIndicatorString(2, "Zombie den location received");
			}
		} 
	}
	
	void addLocation( MapLocation newLocation, LinkedList<MapLocation> list ) {
		Iterator<MapLocation> iterator = list.iterator();
		while( iterator.hasNext() ){
			if( iterator.next().distanceSquaredTo( newLocation ) <= SMALLER_RADIUS  ) {
				iterator.remove();
			}
		}
		list.add( newLocation );
		if( parts.size() > MAX_QUEUE_SIZE ) {
			parts.remove();
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
	////////////////////////////////////////////////////////////////////////
	///////////////// Turns message signals into locations /////////////////
	////////////////////////////////////////////////////////////////////////
	
	// This implementation broadcasts the displacement from the archon to the
	// target location. The displacement is encoded using the
	// positionToInteger() method.
	public static void broadcastLocation(RobotController rc, MapLocation location,
			int messageType, int broadcastRadius ) throws GameActionException {
		int v[] = new int[2];
		v[0] = location.x - rc.getLocation().x;
		v[1] = location.y - rc.getLocation().y;
		rc.broadcastMessageSignal( messageType, positionToInteger(v), broadcastRadius );
	}
	
	private MapLocation messageSignalToLocation( Signal beep ) {
		int[] message = beep.getMessage();
		if ( message != null && message.length >= 2 ) {
			int[] displacement = integerToPosition( beep.getMessage()[1] );
			return beep.getLocation().add( displacement[0], displacement[1] );
		} else {
			return null;
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

}