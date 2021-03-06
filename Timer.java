package artanis;

public class Timer {
	
	Timer( int turns ) {
		timeRemaining = turns;
		totalTime = turns;
	}
	
	private int totalTime;
	private int timeRemaining;
	
	// This method returns true when timeRemaining <= 0.
	public boolean isWaiting() {
		if ( timeRemaining > 0 ) {
			timeRemaining--;
			return true;
		} else {
			return false;
		}
	}
	
	public void reset() {
		timeRemaining = totalTime;
	}
}