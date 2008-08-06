package com.sun.darkstar.example.snowman.game.task.enumn;

/**
 * <code>ETask</code> defines the enumerations of all <code>ITask</code>.
 * <p>
 * The enumeration of an <code>ITask</code> implies the execution logic type
 * of the <code>ITask</code>.
 * 
 * @author Yi Wang (Neakor)
 * @version Creation date: 06-03-2008 11:07 EST
 * @version Modified date: 08-05-2008 14:38 EST
 */
public enum ETask {
	/**
	 * The task used to authenticate the user inputs with the server.
	 */
	Authenticate(ETaskType.RealTime),
	/**
	 * The task used to reset the login state after login attempt failed.
	 */
	ResetLogin(ETaskType.RealTime),
	/**
	 * The task used to change game state.
	 */
	GameState(ETaskType.RealTime),
	/**
	 * The task used to create a new game.
	 */
	NewGame(ETaskType.RealTime),
	/**
	 * The task used to add a MOB.
	 */
	AddMOB(ETaskType.RealTime),
	/**
	 * The task used to initialize chase camera.
	 */
	Ready(ETaskType.RealTime),
	/**
	 * The task used to start the battle.
	 */
	StartGame(ETaskType.RealTime),
	/**
	 * The task used to update all value associated with mouse position.
	 */
	UpdateState(ETaskType.RealTime),
	/**
	 * The task used to initiate the movement of a snowman.
	 */
	MoveCharacter(ETaskType.RealTime),
	/**
	 * The task used to update the HP value of a character.
	 */
	SetHP(ETaskType.RealTime),
	/**
	 * The task used to start the attacking process.
	 */
	Attacking(ETaskType.RealTime),
	/**
	 * The task used to create snow balls.
	 */
	CreateSnowball(ETaskType.Certified),
	/**
	 * The task used to update the motion of snow balls.
	 */
	Throw(ETaskType.RealTime);
	
	/**
	 * The <code>ETaskType</code> enumeration.
	 */
	private final ETaskType type;
	
	/**
	 * Constructor of <code>ETask</code>.
	 * @param type The <code>ETaskType</code> enumeration.
	 */
	private ETask(ETaskType type) {
		this.type = type;
	}
	
	/**
	 * Retrieve the type of this task.
	 * @return The <code>ETaskType</code> enumeration.
	 */
	public ETaskType getType() {
		return this.type;
	}
	
	/**
	 * <code>ETaskType</code> defines all types of <code>ITask</code> managed by the
	 * <code>TaskManager</code>.
	 * 
	 * @author Yi Wang (Neakor)
	 * @author Tim Poliquin (Weenahmen)
	 * @version Creation date: 06-02-2008 15:48 EST
	 * @version Modified date: 06-02-2008 15:50 EST
	 */
	public enum ETaskType {
		/**
		 * The real-time task type.
		 */
		RealTime,
		/**
		 * The certified task type.
		 */
		Certified
	}
}
