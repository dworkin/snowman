package com.sun.darkstar.example.snowman.game.task;

import java.io.IOException;
import java.util.Properties;

import com.jmex.game.state.GameStateManager;
import com.sun.darkstar.example.snowman.game.Game;
import com.sun.darkstar.example.snowman.game.gui.input.KeyInputConverter;
import com.sun.darkstar.example.snowman.game.gui.input.MouseInputConverter;
import com.sun.darkstar.example.snowman.game.state.enumn.EGameState;
import com.sun.darkstar.example.snowman.game.state.scene.LoginState;
import com.sun.darkstar.example.snowman.game.state.scene.login.LoginGUI;
import com.sun.darkstar.example.snowman.game.task.enumn.ETask;

/**
 * <code>LoginTask</code> extends <code>RealTimeTask</code> to login the user
 * into the server application.
 * <p>
 * <code>LoginTask</code> execution logic:
 * 1. Disable all GUI inputs by disabling GUI input conversions.
 * 2. Updates the status label of <code>LoginGUI</code>.
 * 3. Change 'Play' button text to 'Please wait...'.
 * 4. Invoke <code>Client</code> to login to the server. 
 * <p>
 * <code>LoginTask</code> does not have a more detailed 'equals' comparison.
 * All <code>LoginTask</code> are considered 'equal', therefore, a newer
 * version can always replace the older one.
 * 
 * @author Yi Wang (Neakor)
 * @version Creation date: 07-09-2008 17:02 EST
 * @version Modified date: 07-09-2008 15:00 EST
 */
public class LoginTask extends RealTimeTask {
	/**
	 * The status text to display.
	 */
	private final String status;
	/**
	 * The <code>String</code> user name to login with.
	 */
	private final String username;
	/**
	 * The <code>String</code> password to login with.
	 */
	private final String password;
	
	/**
	 * Constructor of <code>LoginTask</code>.
	 * @param game The <code>Game</code> instance.
	 * @param username The <code>String</code> user name to login with.
	 * @param password The <code>String</code> password to login with.
	 */
	public LoginTask(Game game, String username, String password) {
		super(ETask.Login, game);
		this.status = "Waiting for server to match you into a game...";
		this.username = username;
		this.password = password;
	}

	@Override
	public void execute() {
		final LoginGUI gui = ((LoginState)GameStateManager.getInstance().getChild(EGameState.LoginState.toString())).getGUI();
		gui.setStatus(this.status);
		KeyInputConverter.getInstance().setEnabled(false);
		MouseInputConverter.getInstance().setEnabled(false);
		gui.setButtonText("Please wait...");
		System.out.println("Username: " + this.username);
		System.out.println("Password: " + this.password);
//		try {
//			Properties properties = new Properties();
//			properties.setProperty("host", "localhost");
//			properties.setProperty("port", "1138");
//			this.game.getClient().getConnection().login(properties);
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
	}
}