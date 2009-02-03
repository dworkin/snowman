/*
 * Copyright (c) 2008, Sun Microsystems, Inc.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in
 *       the documentation and/or other materials provided with the
 *       distribution.
 *     * Neither the name of Sun Microsystems, Inc. nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.sun.darkstar.example.snowman.server.impl;

import com.sun.darkstar.example.snowman.common.physics.enumn.EForce;
import com.sun.darkstar.example.snowman.server.interfaces.SnowmanPlayer;
import com.sun.darkstar.example.snowman.server.interfaces.SnowmanGame;
import com.sun.darkstar.example.snowman.server.interfaces.SnowmanFlag;
import com.sun.darkstar.example.snowman.common.protocol.messages.ServerMessages;
import com.sun.darkstar.example.snowman.common.protocol.processor.IServerProcessor;
import com.sun.darkstar.example.snowman.common.protocol.enumn.ETeamColor;
import com.sun.darkstar.example.snowman.common.protocol.enumn.EEndState;
import com.sun.darkstar.example.snowman.common.util.HPConverter;
import com.sun.darkstar.example.snowman.common.util.Coordinate;
import com.sun.darkstar.example.snowman.common.util.enumn.EStats;
import com.sun.darkstar.example.snowman.server.service.GameWorldManager;
import com.sun.sgs.app.AppContext;
import com.sun.sgs.app.Channel;
import com.sun.sgs.app.ClientSession;
import com.sun.sgs.app.ManagedReference;
import com.sun.sgs.app.ObjectNotFoundException;
import com.sun.sgs.app.Task;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class is the player's "proxy" in the world of managed
 * objects.  It implements the ClientSessionListener interface so that
 * it can be the reception point for all client session events.
 * For here, it will call other managed ibjects to respond to thsoe
 * events.
 * @author Jeffrey Kesselman
 * @author Owen Kellett
 * @author Yi Wang (Neakor)
 */
public class SnowmanPlayerImpl implements SnowmanPlayer,
                                          Serializable,
                                          IServerProcessor {

    public static final long serialVersionUID = 1L;
    protected static Logger logger = Logger.getLogger(SnowmanPlayerImpl.class.getName());
    static long DEATHDELAYMS = 10 * 1000;
    static float POSITIONTOLERANCESQD = 4.0f;
    static int RESPAWNHP = (int) EStats.SnowmanFullStrength.getValue();
    static int ATTACKHP = (int) EStats.SnowballDamage.getValue();    // The player's session
    private final ManagedReference<ClientSession> sessionRef;    // The game's channel
    private ManagedReference<Channel> channelRef;
    /**
     * Player information and overall statistics
     */
    private final String name;
    private int wins;
    private int losses;
    /**
     * Current game information
     */
    private int id;
    private float startX;
    private float startY;
    private float destX;
    private float destY;
    private long timestamp;
    private ETeamColor teamColor;    // if gameRef == null, player is not yet in a game
    protected ManagedReference<SnowmanGame> gameRef = null;
    protected ManagedReference<SnowmanFlag> holdingFlagRef;
    protected PlayerState state = PlayerState.NONE;
    protected int hitPoints = RESPAWNHP;

    public SnowmanPlayerImpl(String name,
                             ClientSession session) {
        this.name = name;
        sessionRef = session == null ? null : AppContext.getDataManager().createReference(session);
    }

    protected static enum PlayerState {
        //game has not started
        NONE,
        //player is not moving
        STOPPED,
        //player is moving
        MOVING,
        //player is dead
        DEAD
    }

    public void setID(int id) {
        this.id = id;
    }

    public void setLocation(float x, float y) {
        startX = destX = x;
        startY = destY = y;
        this.state = PlayerState.STOPPED;
    }

    public void setTeamColor(ETeamColor color) {
        AppContext.getDataManager().markForUpdate(this);
        teamColor = color;
    }

    public void setGame(SnowmanGame game) {
        assert game != null;
        AppContext.getDataManager().markForUpdate(this);
        gameRef = AppContext.getDataManager().createReference(game);
        channelRef = AppContext.getDataManager().createReference(game.getGameChannel());
    }

    public float ranking() {
        return 1000f * wins / losses;
    }

    public float getX() {
        return startX;
    }

    public float getY() {
        return startY;
    }

    /**
     * This method calculates the projected current position.
     * If the player is not moving, it simply gives the current position as
     * represented by startX and startY.
     * If the player is moving, it calculates the position based on the last
     * known timestamp, the currently given timestamp, the speed (which is
     * calculated according to the current hit points), and the current
     * destination.
     * 
     * @param time
     * @return the position of the player at the given time
     */
    public Coordinate getExpectedPositionAtTime(long time) {
        float currentX = startX;
        float currentY = startY;
        if (state == PlayerState.MOVING) {
            //total time that player has been moving
            long dt = time - timestamp;

            //calculate speed from HPconverter
            float ratePerMs = (EForce.Movement.getMagnitude() / HPConverter.getInstance().convertMass(hitPoints)) * 0.00001f;
            float distanceTraveled = ratePerMs * dt;

            //calculate the actual distance traveled based on the speed
            float dx = destX - startX;
            float dy = destY - startY;
            float targetDistance = (float) Math.sqrt((dx * dx) + (dy * dy));

            //if we've travelled beyond target, target is the position
            if (targetDistance <= distanceTraveled) {
                currentX = destX;
                currentY = destY;
            } //otherwise, we need to calculate the new position based on
            //a system of equations (looking for realDX and realDY as variables):
            //  dx/dy = realDX/realDY
            //  realDX^2 + realDY^2 = distanceTraveled^2
            else {
                float realDX = dx == 0.0f ? 0.0f : (float) Math.sqrt(
                        (distanceTraveled * distanceTraveled) /
                        ((dy * dy) / (dx * dx) + 1));
                float realDY = dy == 0.0f ? 0.0f : (float) Math.sqrt(
                        (distanceTraveled * distanceTraveled) /
                        ((dx * dx) / (dy * dy) + 1));

                //ensure that the signs match for the deltas
                if (dx < 0) {
                    realDX *= -1;
                }
                if (dy < 0) {
                    realDY *= -1;
                }
                currentX = startX + realDX;
                currentY = startY + realDY;
            }
        }
        return new Coordinate(currentX, currentY);
    }

    /**
     * Check that the given coordinates are within the 
     * given tolerance of eachother
     */
    private boolean checkTolerance(float expectedX, float expectedY,
                                   float givenX, float givenY,
                                   float tolerance) {
        float dx = expectedX - givenX;
        float dy = expectedY - givenY;
        float distanceSqd = (dx * dx) + (dy * dy);
        return distanceSqd < tolerance;
    }

    public int getID() {
        return id;
    }

    public void setReadyToPlay(boolean readyToPlay) {
        AppContext.getDataManager().markForUpdate(this);

        if (readyToPlay) {
            this.state = PlayerState.STOPPED;
        } else {
            this.state = PlayerState.NONE;
        }
    }

    public boolean getReadyToPlay() {
        return this.state != PlayerState.NONE;
    }

    // Send a message to this player
    public void send(ByteBuffer buff) {
        buff.flip();
        sessionRef.get().send(buff);
    }

    // Send a message to all the players
    private void sendAll(ByteBuffer buff) {
        buff.flip();
        channelRef.get().send(null, buff);
    }

    public ClientSession getSession() {
        try {
            return sessionRef.get();
        } catch (ObjectNotFoundException disconnected) {
            return null;
        }
    }

    public boolean isServerSide() {
        return false;
    }

    // IServerProcessor Messages
    public void ready() {
        if (gameRef != null) {
            setReadyToPlay(true);
            gameRef.get().startGameIfReady();
        }
    }

    public void moveMe(float startx, float starty,
                       float endx, float endy) {
        //verify that the start location is valid
        Long now = System.currentTimeMillis();
        moveMe(now, startx, starty, endx, endy);
    }

    protected void moveMe(long now,
                          float startx, float starty,
                          float endx, float endy) {
        //no op if player is dead or not in a game
        if (state == PlayerState.DEAD || state == PlayerState.NONE) {
            return;
        }
        AppContext.getDataManager().markForUpdate(this);

        //verify that the start location is valid
        Coordinate expectedPosition = this.getExpectedPositionAtTime(now);

        if (checkTolerance(expectedPosition.getX(), expectedPosition.getY(),
                           startx, starty,
                           POSITIONTOLERANCESQD)) {
            //collision detection
            Coordinate trimPosition = AppContext.getManager(GameWorldManager.class).
                    trimPath(new Coordinate(startx, starty),
                             new Coordinate(endx, endy));

            this.timestamp = now;
            this.startX = startx;
            this.startY = starty;
            this.destX = trimPosition.getX();
            this.destY = trimPosition.getY();
            this.state = PlayerState.MOVING;

            sendAll(ServerMessages.createMoveMOBPkt(id, startX, startY, destX, destY));
        } else {
            logger.log(Level.FINE, "move from {0} failed start position check", name);

            this.timestamp = now;
            this.setLocation(expectedPosition.getX(), expectedPosition.getY());
            sendAll(ServerMessages.createStopMOBPkt(id,
                                                    expectedPosition.getX(),
                                                    expectedPosition.getY()));
        }
    }

    public void attack(int targetID, float x, float y) {
        Long now = System.currentTimeMillis();
        attack(now, targetID, x, y);
    }

    protected void attack(long now, int targetID, float x, float y) {
        //no op if player is dead or not in a game
        if (state == PlayerState.DEAD || state == PlayerState.NONE) {
            return;
        }
        AppContext.getDataManager().markForUpdate(this);

        //verify that the start location is valid
        Coordinate expectedPosition = this.getExpectedPositionAtTime(now);

        if (checkTolerance(expectedPosition.getX(), expectedPosition.getY(),
                           x, y, POSITIONTOLERANCESQD)) {
            //get the target player and determine its location
            SnowmanPlayer target = gameRef.get().getPlayer(targetID);

            if (target == null) {
                return; // player no longer in game
            }
            Coordinate targetPosition = target.getExpectedPositionAtTime(now);

            boolean success = true;
            //verify that target is in range
            float range = HPConverter.getInstance().convertRange(hitPoints);
            if (!checkTolerance(expectedPosition.getX(), expectedPosition.getY(),
                                targetPosition.getX(), targetPosition.getY(),
                                range * range)) {
                logger.log(Level.FINE, "attack from {0} out of range", name);
                success = false;
            }

            //collision detection
            if (!AppContext.getManager(GameWorldManager.class).validThrow(new Coordinate(x, y),
                                                                          targetPosition)) {
                logger.log(Level.FINE, "attack from {0} detected a collision", name);
                success = false;
            }

            //perform implicit stop
            this.timestamp = now;
            this.setLocation(x, y);

            if (success) {
                //stop the target
                target.setLocation(targetPosition.getX(), targetPosition.getY());
                sendAll(ServerMessages.createAttackedPkt(id,
                                                         targetID,
                                                         target.hit(ATTACKHP, targetPosition.getX(), targetPosition.getY())));
            } else {
                sendAll(ServerMessages.createAttackedPkt(id, targetID, 0));
            }
        } else {
            //ignore an invalid attack
            logger.log(Level.FINE, "attack from {0} failed attack position check", name);
        }
    }

    public void getFlag(int flagID, float x, float y) {
        Long now = System.currentTimeMillis();
        getFlag(now, flagID, x, y);
    }

    protected void getFlag(long now, int flagID, float x, float y) {
        //no op if player is dead or not in a game
        if (state == PlayerState.DEAD || state == PlayerState.NONE) {
            return;
        }
        SnowmanFlag flag = gameRef.get().getFlag(flagID);

        // Can not get flag if same team or flag is held by another player
        if (flag == null ||
                flag.getTeamColor() == teamColor ||
                flag.isHeld() ||
                holdingFlagRef != null) {
            return;        //verify that the start location is valid
        }
        Coordinate expectedPosition = this.getExpectedPositionAtTime(now);
        if (checkTolerance(expectedPosition.getX(), expectedPosition.getY(),
                           x, y, POSITIONTOLERANCESQD)) {

            //verify that the player is in range of the flag
            if (checkTolerance(x, y, flag.getX(), flag.getY(),
                               EStats.GrabRange.getValue() * EStats.GrabRange.getValue())) {
                AppContext.getDataManager().markForUpdate(this);

                //perform implicit stop
                this.timestamp = now;
                this.setLocation(x, y);

                //attach the flag
                flag.setHeldBy(this);
                holdingFlagRef = AppContext.getDataManager().createReference(flag);
                sendAll(ServerMessages.createAttachObjPkt(flagID, id));
            } else {
                logger.log(Level.FINER, "get flag from {0} failed radius check", name);
            }
        } else {
            logger.log(Level.FINE, "get flag from {0} failed position check", name);
        }
    }

    public void score(float x, float y) {
        Long now = System.currentTimeMillis();
        score(now, x, y);
    }

    /**
     * Attempt to score. If sucessful return true. Note that a true
     * return may mean that the currentGameRef will no longer resolve
     * to a game object
     * @param now
     * @param x
     * @param y
     * @return success
     */
    protected boolean score(long now, float x, float y) {
        //no op if player is dead or not in a game
        if (state == PlayerState.DEAD || state == PlayerState.NONE) {
            return false;        //ignore if we aren't holding the flag
        }
        if (holdingFlagRef == null) {
            logger.log(Level.FINE, "score from {0} failed, not holding flag", name);
            return false;
        }

        //verify that the start location is valid
        Coordinate expectedPosition = this.getExpectedPositionAtTime(now);
        if (checkTolerance(expectedPosition.getX(), expectedPosition.getY(),
                           x, y, POSITIONTOLERANCESQD)) {
            SnowmanFlag flag = holdingFlagRef.get();

            //verify that the player is in range of the score position
            if (checkTolerance(x, y, flag.getGoalX(), flag.getGoalY(),
                               EStats.GoalRadius.getValue() * EStats.GoalRadius.getValue())) {
                // end game 
                gameRef.get().endGame(teamColor == ETeamColor.Red ? EEndState.RedWin : EEndState.BlueWin);
                return true;
            } else {
                logger.log(Level.FINER, "score from {0} failed radius check", name);
            }
        } else {
            logger.log(Level.FINE, "score from {0} failed position check", name);
        }
        return false;
    }

    // respawn
    public void respawn() {
        if (gameRef != null) {
            AppContext.getDataManager().markForUpdate(this);
            hitPoints = RESPAWNHP;
            Coordinate position = SnowmanMapInfo.getRespawnPosition(SnowmanMapInfo.DEFAULT, this.getTeamColor());
            setLocation(position.getX(), position.getY());
            sendAll(ServerMessages.createRespawnPkt(id, position.getX(), position.getY()));
        }
    }

    public int hit(int hp, float attackX, float attackY) {
        if (hitPoints > 0) { // not already dead
            AppContext.getDataManager().markForUpdate(this);

            hitPoints -= hp;
            if (hitPoints <= 0) { // newly dead
                // drop flag
                SnowmanFlag flag = holdingFlagRef == null ? null : holdingFlagRef.get();
                if (flag != null) {
                    flag.drop(attackX, attackY);
                }
                holdingFlagRef = null;
                state = PlayerState.DEAD;

                // schedule respawn
                AppContext.getTaskManager().scheduleTask(
                        new RespawnTask(AppContext.getDataManager().createReference((SnowmanPlayer) this)),
                        DEATHDELAYMS);
            }
        }
        return hp;
    }

    public int getHitPoints() {
        return hitPoints;
    }

    public void dropFlag() {
        SnowmanFlag flag = holdingFlagRef == null ? null : holdingFlagRef.get();
        if (flag != null) {
            Coordinate expectedPosition = this.getExpectedPositionAtTime(System.currentTimeMillis());
            flag.drop(expectedPosition.getX(), expectedPosition.getY());
        }
        holdingFlagRef = null;
    }

    static private class RespawnTask implements Task, Serializable {

        /**
         * Serial version.
         */
        private static final long serialVersionUID = -6040022484794307971L;
        final ManagedReference<SnowmanPlayer> playerRef;

        RespawnTask(ManagedReference<SnowmanPlayer> playerRef) {
            this.playerRef = playerRef;
        }

        public void run() throws Exception {
            try {
                playerRef.get().respawn();
            } catch (ObjectNotFoundException gameDone) {
            }
        }
    }

    public SnowmanGame getGame() {
        return gameRef == null ? null : gameRef.get();
    }

    public String getName() {
        return name;
    }

    public IServerProcessor getProcessor() {
        return this;
    }

    public ETeamColor getTeamColor() {
        return teamColor;
    }

    public void removingObject() {
        if (sessionRef != null) {
            try {
                AppContext.getDataManager().removeObject(sessionRef.get());
            } catch (ObjectNotFoundException alreadyDisconnected) {
            } // workaround bug is ClientSessionImpl
            catch (IllegalStateException workAroundIssue87) {
            }
        }
    }

    public void chatMessage(String message) {
        // Create new packet with ID.
        sendAll(ServerMessages.createChatPkt(id, message));
    }
}