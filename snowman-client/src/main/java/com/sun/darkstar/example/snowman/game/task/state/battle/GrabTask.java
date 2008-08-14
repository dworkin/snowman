package com.sun.darkstar.example.snowman.game.task.state.battle;

import com.sun.darkstar.example.snowman.common.entity.enumn.EEntity;
import com.sun.darkstar.example.snowman.common.entity.view.View;
import com.sun.darkstar.example.snowman.common.interfaces.IEntity;
import com.sun.darkstar.example.snowman.common.protocol.messages.ClientMessages;
import com.sun.darkstar.example.snowman.game.Game;
import com.sun.darkstar.example.snowman.game.entity.DynamicEntity;
import com.sun.darkstar.example.snowman.game.entity.scene.CharacterEntity;
import com.sun.darkstar.example.snowman.game.entity.scene.SnowmanEntity;
import com.sun.darkstar.example.snowman.game.entity.util.EntityManager;
import com.sun.darkstar.example.snowman.game.entity.view.scene.CharacterView;
import com.sun.darkstar.example.snowman.game.entity.view.util.ViewManager;
import com.sun.darkstar.example.snowman.game.state.enumn.EGameState;
import com.sun.darkstar.example.snowman.game.task.RealTimeTask;
import com.sun.darkstar.example.snowman.game.task.enumn.ETask;

/**
 * <code>GrabTask</code> extends <code>RealTimeTask</code> to define the
 * task that grabs a flag.
 * <p>
 * <code>GrabTask</code> execution logic:
 * 1. Retrieve the views of both the target and flag.
 * 2. Attach the flag view to the target view with appropriate translation.
 * 3. Send out 'grab' packet.
 * <p>
 * <code>GrabTask</code> are considered 'equal' if and only if the grabbing
 * source ID and the flag ID are 'equal'.
 * 
 * @author Yi Wang (Neakor)
 * @version Creation date: 08-14-2008 12:09 EST
 * @version Modified date: 08-14-2008 12:50 EST
 */
public class GrabTask extends RealTimeTask {
	/**
	 * The ID number of the flag.
	 */
	private final int flagID;
	/**
	 * The ID number of the target to attach to.
	 */
	private final int targetID;

	/**
	 * Constructor of <code>GrabTask</code>.
	 * @param game The <code>Game</code> instance.
	 * @param flagID The ID number of the flag.
	 * @param targetID The ID number of the target to attach to.
	 */
	public GrabTask(Game game, int flagID, int targetID) {
		super(ETask.Grab, game);
		this.flagID = flagID;
		this.targetID = targetID;
	}

	@Override
	public void execute() {
		// Step 1.
		IEntity target = EntityManager.getInstance().getEntity(this.targetID);
		IEntity flag = EntityManager.getInstance().getEntity(this.flagID);
		View targetView = (View)ViewManager.getInstance().getView(target);
		View flagView = (View)ViewManager.getInstance().getView(flag);
		// Step 2.
		if(target instanceof CharacterEntity) {
			flagView.setLocalTranslation(0.3f, 0.5f, 0);
			((CharacterEntity)target).setFlag((DynamicEntity)flag);
			flagView.detachFromParent();
			flagView.attachTo(targetView);
		} else if(target.getEnumn() == EEntity.Terrain && flagView.getParent() instanceof CharacterView) {
			flagView.getLocalTranslation().set(flagView.getParent().getLocalTranslation());
			flagView.detachFromParent();
			flagView.attachTo(this.game.getGameState(EGameState.BattleState).getWorld().getDynamicRoot());
		}
		// Step 3.
		if(target instanceof SnowmanEntity) {
			this.game.getClient().send(ClientMessages.createGetFlagPkt(this.flagID, targetView.getLocalTranslation().x, targetView.getLocalTranslation().z));
		}
	}
	
	@Override
	public boolean equals(Object object) {
		if(super.equals(object)) {
			if(object instanceof GrabTask) {
				GrabTask given = (GrabTask)object;
				return (given.flagID == this.flagID) && (given.targetID == this.targetID);
			}
		}
		return false;
	}
}
