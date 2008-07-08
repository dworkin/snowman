package com.sun.darkstar.example.snowman.game;

import java.net.URL;

import com.jme.app.BaseGame;
import com.jme.input.KeyBindingManager;
import com.jme.input.KeyInput;
import com.jme.math.Vector3f;
import com.jme.renderer.Camera;
import com.jme.renderer.ColorRGBA;
import com.jme.renderer.pass.BasicPassManager;
import com.jme.system.DisplaySystem;
import com.jme.util.Timer;
import com.jmex.game.state.GameStateManager;
import com.sun.darkstar.example.snowman.client.Client;
import com.sun.darkstar.example.snowman.exception.MissingComponentException;
import com.sun.darkstar.example.snowman.game.physics.util.PhysicsManager;
import com.sun.darkstar.example.snowman.game.task.util.TaskManager;
import com.sun.darkstar.example.snowman.interfaces.IComponent;

/**
 * <code>Game</code> represents the client end application which maintains
 * the basic game structure including all the <code>GameState</code>, and
 * other utility instances.
 * <p>
 * <code>Game</code> is responsible for updating all the singleton manager
 * systems including <code>GameStateManager</code>, <code>BasicPassManager</code>,
 * <code>PhysicsManager</code> and <code>TaskManager</code>.
 * <p>
 * <code>Game</code> needs to be connected with <code>Client</code> before
 * initialization in order to send out packets to the <code>Server</code>.
 * 
 * @author Yi Wang (Neakor)
 * @version Creation date: 05-23-2008 14:02 EST
 * @version Modified date: 06-06-2008 17:30 EST
 */
public class Game extends BaseGame implements IComponent{
	/**
	 * The flag indicates the activeness of this <code>Component</code>.
	 */
	private boolean active;
	/**
	 * The <code>Client</code> instance.
	 */
	private Client client;
	/**
	 * The <code>Timer</code> instance.
	 */
	private Timer timer;
	/**
	 * The <code>TaskManager</code> instance.
	 */
	private TaskManager taskManager;
	/**
	 * The <code>PhysicsManager</code> instance.
	 */
	private PhysicsManager physicsManager;
	/**
	 * The <code>GameStateManager</code> instance.
	 */
	private GameStateManager stateManager;
	/**
	 * The <code>BasicPassManager</code> instance.
	 */
	private BasicPassManager passManager;
	/**
	 * The update interpolation value.
	 */
	private float interpolation;
	/**
	 * The screen shot count value.
	 */
	private int count;
	
	/**
	 * Constructor of <code>Game</code>.
	 */
	public Game() {
		super();
	}

	@Override
	public void activate() throws MissingComponentException {
		if(this.validate()) {
			this.active = true;
			this.initialize();
			this.start();
		}
	}

	@Override
	public boolean validate() throws MissingComponentException {
		if(this.client == null) {
			throw new MissingComponentException(Client.class.toString());
		}
		return true;
	}

	@Override
	public void initialize() {
		this.setConfigShowMode(ConfigShowMode.AlwaysShow, (URL)null); // TODO
	}

	@Override
	public void connect(IComponent component) {
		if(component instanceof Client) {
			this.client = (Client)component;
		}
	}

	@Override	
	protected void initSystem() {
		this.display = DisplaySystem.getDisplaySystem(this.settings.getRenderer());
		this.display.setTitle("Snowman");
//		try {
//			Texture icon = AssetLoader.getInstance().loadTexture(TextureData.Icon_Big);
//			Texture iconSmall = AssetLoader.getInstance().loadTexture(TextureData.Icon_Small);
//			this.display.setIcon(new Image[]{icon.getImage(), iconSmall.getImage()});
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
		this.initializeWindow();
		this.initializeCamera();
		this.initializeHotkey();
		this.timer = Timer.getTimer();
		this.taskManager = TaskManager.getInstance();
		this.physicsManager = PhysicsManager.getInstance();
		this.stateManager = GameStateManager.create();
		this.passManager = new BasicPassManager();
	}
	
	/**
	 * Initialize the window. If this is called to apply new game settings, the window
	 * is recreated.
	 */
	private void initializeWindow() {
		this.display.setMinSamples(0);
		this.display.setVSyncEnabled(false);
		this.display.createWindow(this.settings.getWidth(), this.settings.getHeight(), this.settings.getDepth(),
				this.settings.getFrequency(), this.settings.isFullscreen());
		this.display.getRenderer().setBackgroundColor(ColorRGBA.black);
	}

	/**
	 * Initialize the camera.
	 */
	private void initializeCamera() {
		// Create the camera.
		Camera camera = this.display.getRenderer().createCamera(this.display.getWidth(), this.display.getHeight());
		camera.setFrustumPerspective(45.0f, this.display.getWidth()/this.display.getHeight(), 1, 1000);
		Vector3f location = new Vector3f(0.0f, 0.0f, 0.0f);
		Vector3f left = new Vector3f(-1.0f, 0.0f, 0.0f);
		Vector3f up = new Vector3f(0.0f, 1.0f, 0.0f);
		Vector3f direction = new Vector3f(0.0f, 0.0f, -1.0f);
		camera.setFrame(location, left, up, direction);
		camera.setParallelProjection(false);
		camera.update();
		// Assign Camera.
		this.display.getRenderer().setCamera(camera);
	}

	/**
	 * Initialize the hot keys.
	 */
	private void initializeHotkey() {
		KeyBindingManager.getKeyBindingManager().set("exit", KeyInput.KEY_ESCAPE);
		KeyBindingManager.getKeyBindingManager().set("screenshot", KeyInput.KEY_F1);
	}

	@Override
	protected void initGame() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void update(float interpolation) {
		// Update the timer to get the frame rate.
		this.timer.update();
		this.interpolation = this.timer.getTimePerFrame();
		// Execute tasks.
		this.taskManager.update();
		// Update physics.
		this.physicsManager.update(this.interpolation);
		// Update the game states.
		this.stateManager.update(this.interpolation);
		// Update the pass manager to update special effect passes.
		this.passManager.updatePasses(this.interpolation);
		// Update basic hot keys.
		if(KeyBindingManager.getKeyBindingManager().isValidCommand("exit", false)) {
			this.finish();		
		} else if(KeyBindingManager.getKeyBindingManager().isValidCommand("screenshot", false)) {
			this.display.getRenderer().takeScreenShot("Snowman" + this.count);
			this.count++;
		}
	}

	@Override
	protected void render(float interpolation) {
		this.display.getRenderer().clearBuffers();
		// Render all render passes.
		this.passManager.renderPasses(this.display.getRenderer());
	}

	@Override
	@SuppressWarnings("unused")
	protected void reinit() {}

	@Override
	protected void cleanup() {
		if(this.stateManager != null) this.stateManager.cleanup();
		if(this.passManager != null) this.passManager.cleanUp();
	}

	@Override
	public void deactivate() {
		this.active = false;
	}

	@Override
	public boolean isActive() {
		return this.active;
	}
}