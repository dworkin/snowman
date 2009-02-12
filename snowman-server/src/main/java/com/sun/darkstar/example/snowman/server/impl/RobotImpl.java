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

import com.sun.darkstar.example.snowman.common.util.Coordinate;
import com.sun.darkstar.example.snowman.common.util.HPConverter;
import com.sun.darkstar.example.snowman.server.interfaces.SnowmanFlag;
import com.sun.darkstar.example.snowman.server.interfaces.SnowmanGame;
import com.sun.darkstar.example.snowman.server.interfaces.SnowmanPlayer;
import com.sun.sgs.app.AppContext;
import com.sun.sgs.app.ClientSession;
import com.sun.sgs.app.ManagedReference;
import com.sun.sgs.app.ObjectNotFoundException;
import com.sun.sgs.app.Task;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Random;

/**
 * Robot player
 * @author kbt
 */
public class RobotImpl extends SnowmanPlayerImpl {

    private static final long serialVersionUID = 1L;
        
    private final int moveDelay;
    private final Random random;
    
    // Player IDs of potential targets
    private ArrayList<Integer> potentialTargets = null;
    
    // reference to opponent flag
    private ManagedReference<SnowmanFlag> theirFlagRef = null;
    
    public RobotImpl(String name, int delay) {
        super(name, null);
        moveDelay = delay;
        random = new Random(name.hashCode());
        scheduleMove(10000);// TODO need to find out when the game starts
    }
    
    private void scheduleMove(int delay) {
        AppContext.getTaskManager().scheduleTask(
                new MoveTask(
                AppContext.getDataManager().createReference((RobotImpl)this)),
                delay + random.nextInt(500));
    }
    
    private void moveRobot() {

        if (gameRef == null) // game over
            return;
        
        if (state == PlayerState.NONE || state == PlayerState.DEAD) {
            scheduleMove(moveDelay * 4);
            return;
        }
        
        // setup list of potential targets
        // grab all players for now, teammates will be removed later
        if (potentialTargets == null) {
            AppContext.getDataManager().markForUpdate(this);
            SnowmanGame game = gameRef.get();
            potentialTargets = new ArrayList<Integer>();
            potentialTargets.addAll(game.getPlayerIds());
            
            ArrayList<Integer> potentialFlagTargets = new ArrayList<Integer>();
            potentialFlagTargets.addAll(game.getFlagIds());
            
            // find a target flag to go after
            while (theirFlagRef == null) {
                assert potentialFlagTargets.size() > 0;
                Integer targetId = random.nextInt(potentialFlagTargets.size());
                SnowmanFlag flag = gameRef.get().getFlag(
                        potentialFlagTargets.get(targetId));
                if(flag.getTeamColor() != this.getTeamColor()) {
                    theirFlagRef = 
                            AppContext.getDataManager().createReference(flag);
                }
                else {
                    potentialFlagTargets.remove(targetId);
                }
            }
        }
                
        long now = System.currentTimeMillis();
        Coordinate currentPos = getExpectedPositionAtTime(now);
        
        // If holding the flag, try to score
        if (holdingFlagRef != null) {
            if (score(now, currentPos.getX(), currentPos.getY()))
                return; // game over
                  
            // every once in a while, move randomly so that we don't
            // get stuck behind an object
            if (random.nextBoolean() || random.nextBoolean())
                moveMe(now,
                       currentPos.getX(), currentPos.getY(),
                       theirFlagRef.get().getGoalX() + 5 * (random.nextFloat() - 0.5f),
                       theirFlagRef.get().getGoalY() + 5 * (random.nextFloat() - 0.5f));
            else
                moveMe(now,
                       currentPos.getX(), currentPos.getY(),
                       currentPos.getX() + 10 * (random.nextFloat() - 0.5f),
                       currentPos.getY() + 10 * (random.nextFloat() - 0.5f));
            
        // randomly go after the flag
        } else if (random.nextBoolean() && !theirFlagRef.get().isHeld()) {
            SnowmanFlag flag = theirFlagRef.get();
            getFlag(now, flag.getID(), currentPos.getX(), currentPos.getY());
            // if we didn't get it, move towards it
            if (holdingFlagRef == null)
                moveMe(now,
                       currentPos.getX(), currentPos.getY(),
                       flag.getX() + 5 * (random.nextFloat() - 0.5f),
                       flag.getY() + 5 * (random.nextFloat() - 0.5f));
        
        // else just move towards a target snowman
        } else {
            SnowmanGame game = gameRef.get();
            
            int targetIndex = random.nextInt(potentialTargets.size());
            SnowmanPlayer target =
                    game.getPlayer(potentialTargets.get(targetIndex));
            
            // don't attack friendly snowmen or snowmen no longer around
            if((target != null) && (target.getTeamColor() == this.getTeamColor())) {
                potentialTargets.remove(targetIndex);
                target = null;
            }
            
            // If a target is available, move towards it. Attack if it's
            // within range
            if (target != null) {
                Coordinate targetPos = target.getExpectedPositionAtTime(now);

                float dx = currentPos.getX() - targetPos.getX();
                float dy = currentPos.getY() - targetPos.getY();
                float range = HPConverter.getInstance().convertRange(hitPoints);
                if (((dx * dx) + (dy * dy)) < (range * range) &&
                        random.nextBoolean()) {
                    attack(now, target.getID(), currentPos.getX(), currentPos.getY());
                } else {
                    moveMe(now,
                           currentPos.getX(), currentPos.getY(),
                           targetPos.getX() + 10 * (random.nextFloat() - 0.5f),
                           targetPos.getY() + 10 * (random.nextFloat() - 0.5f));
                }
            } else
                moveMe(now,
                       currentPos.getX(), currentPos.getY(),
                       currentPos.getX() + 10 * (random.nextFloat() - 0.5f),
                       currentPos.getY() + 10 * (random.nextFloat() - 0.5f));
        }
        scheduleMove(moveDelay);
    }
    
    static private class MoveTask implements Task, Serializable {
        private static final long serialVersionUID = 1L;
        final ManagedReference<RobotImpl> robotRef;
        
        MoveTask(ManagedReference<RobotImpl> robotRef) {
            this.robotRef = robotRef;
        }

        public void run() throws Exception {
            try {
                robotRef.get().moveRobot();
            } catch (ObjectNotFoundException gameDone) {}
        }
    }
    
    // There is no session associated with the robot, so related methods
    // are overriden with noops
    
    /** {@inheritDoc} */
    @Override
    public void send(ByteBuffer buff) {}
    
    /** {@inheritDoc} */
    @Override
    public ClientSession getSession() {
        return null;
    }
    
    /** {@inheritDoc} */
    @Override
    public boolean isServerSide() {
        return true;
    }
}