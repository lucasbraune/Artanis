package artanis;

public class Timer {
	
	Timer( int turns ) {
		timeRemaining = 0;
		totalTime = turns;
	}
	
	private static int totalTime;
	private static int timeRemaining;
	
	// This method returns true when timeRemaining <= 0.
	boolean isWaiting() {
		if ( timeRemaining > 0 ) {
			timeRemaining--;
			return true;
		} else {
			reset();
			return false;
		}
	}
	
	void reset() {
		timeRemaining = totalTime;
	}
}