package com.sun.darkstar.example.snowman.game.entity.influence.enumn;

/**
 * <code>EInfluence</code> defines the enumerations of all types of influences
 * that <code>IStaticEntity</code> may have.
 * 
 * @author Yi Wang (Neakor)
 * @version Creation date: 06-06-2008 11:12 EST
 * @version Modified date: 06-09-2008 12:01 EST
 */
public enum EInfluence {
	/**
	 * <code>Burn</code> influence damages the target, reduces the
	 * size of the target and increases the motion of the target including
	 * both animation speed and movement speed.
	 * @param damage The reduction in HP.
	 * @param reduceScale The reduction percentage in scale.
	 * @param increase The percentage increase in motion.
	 */
	Burned,
	/**
	 * <code>Slippery</code> influence slows down the target in movement
	 * speed.
	 * @param percentage The reduction percentage in movement speed.
	 */
	Slippery,
}
