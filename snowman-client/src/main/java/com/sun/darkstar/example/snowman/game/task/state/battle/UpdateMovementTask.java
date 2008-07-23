package com.sun.darkstar.example.snowman.game.task.state.battle;

import java.io.IOException;

import com.jme.math.Vector3f;
import com.sun.darkstar.example.snowman.common.entity.view.View;
import com.sun.darkstar.example.snowman.common.protocol.ClientProtocol;
import com.sun.darkstar.example.snowman.exception.ObjectNotFoundException;
import com.sun.darkstar.example.snowman.game.Game;
import com.sun.darkstar.example.snowman.game.entity.scene.SnowmanEntity;
import com.sun.darkstar.example.snowman.game.entity.view.util.ViewManager;
import com.sun.darkstar.example.snowman.game.physics.enumn.EForce;
import com.sun.darkstar.example.snowman.game.physics.util.PhysicsManager;
import com.sun.darkstar.example.snowman.game.task.RealTimeTask;
import com.sun.darkstar.example.snowman.game.task.enumn.ETask;

/**
 * <code>UpdateMovementTask</code> extends <code>RealTimeTask</code> to check
 * if the snowman has reached its destination.
 * <p>
 * <code>UpdateMovementTask</code> execution logic:
 * 1. Check if the snowman has a destination. Return if not.
 * 2. Check if the snowman has reached the area within the tolerance value
 * of the destination.
 * 	  1. If yes, then set the destination to null send out a 'stopme'
 *    message and return.
 *    2. Otherwise add a movement force the direction of the destination
 *    and mark the snowman for update with <code>PhysicsManager</code>.
 * <p>
 * <code>UpdateMovementTask</code> does not have detailed 'equal' comparison.
 * All instances of <code>UpdateMovementTask</code> are considered 'equal'.
 * 
 * @author Yi Wang (Neakor)
 * @version Creation date: 07-22-2008 17:01 EST
 * @version Modified date: 07-22-2008 17:20 EST
 */
public class UpdateMovementTask extends RealTimeTask {
	/**
	 * The <code>SnowmanEntity</code> instance.
	 */
	private final SnowmanEntity snowman;
	/**
	 * The float distance tolerance value.
	 */
	private final float tolerance;

	/**
	 * Constructor of <code>CheckStopTask</code>.
	 * @param game The <code>Game</code> instance.
	 * @param snowman The <code>SnowmanEntity</code> instance.
	 * @param tolerance The float distance tolerance value.
	 */
	public UpdateMovementTask(Game game, SnowmanEntity snowman, float tolerance) {
		super(ETask.UpdateMovement, game);
		this.snowman = snowman;
		this.tolerance = tolerance;
	}

	@Override
	public void execute() {
		if(this.snowman.getDestination() == null) return;
		try {
			View view = (View)ViewManager.getInstance().getView(this.snowman);
			if(this.validatePosition(view)) {
				this.snowman.setDestination(null);
				this.game.getClient().getConnection().send(ClientProtocol.getInstance().createStopMePkg(
						view.getLocalTranslation().x, view.getLocalTranslation().z));
				return;
			} else {
				Vector3f direction = this.snowman.getDestination().subtract(view.getLocalTranslation()).normalizeLocal();
				Vector3f force = direction.multLocal(EForce.Movement.getMagnitude());
				this.snowman.addForce(force);
				PhysicsManager.getInstance().markForUpdate(this.snowman);
			}
		} catch (ObjectNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	/**
	 * Validate if the current position is within the tolerance range of the destination.
	 * @return True if the snowman is considered reached the destination. False otherwise.
	 */
	private boolean validatePosition(View view) {
		float dx = view.getLocalTranslation().x - this.snowman.getDestination().x;
		float dz = view.getLocalTranslation().z - this.snowman.getDestination().z;
		if((dx * dx) + (dz * dz) <= this.tolerance) return true;
		return false;
	}
}