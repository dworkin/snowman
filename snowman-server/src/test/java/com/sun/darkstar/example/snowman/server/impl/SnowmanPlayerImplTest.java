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

import com.sun.darkstar.example.snowman.server.context.MockAppContext;
import com.sun.darkstar.example.snowman.server.context.SnowmanAppContextFactory;
import com.sun.darkstar.example.snowman.server.context.SnowmanAppContext;
import com.sun.darkstar.example.snowman.server.interfaces.SnowmanGame;
import com.sun.darkstar.example.snowman.common.protocol.messages.ServerMessages;
import com.sun.darkstar.example.snowman.common.protocol.enumn.ETeamColor;
import com.sun.darkstar.example.snowman.common.physics.enumn.EForce;
import com.sun.darkstar.example.snowman.common.util.HPConverter;
import com.sun.sgs.app.ClientSession;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import org.junit.Assert;
import org.easymock.EasyMock;
import java.lang.reflect.Field;

/**
 * Test the SnowmanPlayerImpl
 * 
 * @author Owen Kellett
 */
public class SnowmanPlayerImplTest 
{
    private int testPlayerId = 1;
    private ETeamColor testPlayerColor = ETeamColor.Red;
    private SnowmanPlayerImpl testPlayer;
    private SnowmanGame currentGame;
    private ClientSession session;
    private SnowmanAppContext appContext;
    
    private int attackeeId = 2;
    private ETeamColor attackeeColor = ETeamColor.Blue;
    private SnowmanPlayerImpl attackee;
    private ClientSession attackeeSession;

    @Before
    public void initializeContextAndPlayer()
    {
        //create the context
        MockAppContext.create();
        
        //setup the player
        appContext = SnowmanAppContextFactory.getAppContext();
        session = EasyMock.createNiceMock(ClientSession.class);
        currentGame = EasyMock.createNiceMock(SnowmanGame.class);
        EasyMock.replay(session);
        EasyMock.replay(currentGame);
        
        testPlayer = new SnowmanPlayerImpl(appContext, session);
        testPlayer.setGame(currentGame);
        testPlayer.setID(testPlayerId);
        testPlayer.setTeamColor(testPlayerColor);
    }
    
    @After
    public void takeDownContext()
    {
        SnowmanAppContextFactory.setAppContext(null);
    }
    
    /**
     * Test the processing of a MOVEME packet when the player is in
     * the stopped position and the client sends a start position that is
     * in range
     */
    @Test
    public void testMoveMePlayerStoppedAndValidStart()
            throws Exception
    {
        //setup the test players current state
        float startX = 5.0f;
        float startY = 10.0f;
        testPlayer.setReadyToPlay(true);
        testPlayer.setLocation(startX, startY);

        //choose a position within the tolerance
        float targetDistanceSqd = SnowmanPlayerImpl.POSITIONTOLERANCESQD/2.0f;
        float xOffset = (float)Math.sqrt(targetDistanceSqd/2.0f);
        float yOffset = (float) Math.sqrt(targetDistanceSqd / 2.0f);
        float newX = startX+xOffset;
        float newY = startY-yOffset;
        
        //choose a destination
        float destX = 20.0f;
        float destY = 15.0f;
        
        //record timestamp for later verification
        Long timestamp = System.currentTimeMillis();
        
        //setup expected broadcast messages to the game
        EasyMock.resetToDefault(currentGame);
        currentGame.send(null, ServerMessages.createMoveMOBPkt(testPlayerId, newX, newY, destX, destY));
        EasyMock.replay(currentGame);
        
        //make the move
        testPlayer.moveMe(newX, newY, destX, destY);
        
        //verify player information has transitioned properly
        verifyState(testPlayer, SnowmanPlayerImpl.PlayerState.MOVING);
        verifyTimestamp(testPlayer, timestamp);
        verifyLocation(testPlayer, newX, newY);
        verifyDestination(testPlayer, destX, destY);
        
        //verify message has been sent
        EasyMock.verify(currentGame);
    }
    
    
    /**
     * Test the processing of a MOVEME packet when the player is in
     * the stopped position and the client sends a start position that is
     * out of range
     */
    @Test
    public void testMoveMePlayerStoppedAndInvalidStart()
            throws Exception
    {
        //setup the test players current state
        float startX = 5.0f;
        float startY = 10.0f;
        testPlayer.setReadyToPlay(true);
        testPlayer.setLocation(startX, startY);

        //choose a position within the tolerance
        float targetDistanceSqd = SnowmanPlayerImpl.POSITIONTOLERANCESQD*2.0f;
        float xOffset = (float)Math.sqrt(targetDistanceSqd/2.0f);
        float yOffset = (float) Math.sqrt(targetDistanceSqd / 2.0f);
        float newX = startX+xOffset;
        float newY = startY-yOffset;
        
        //choose a destination
        float destX = 20.0f;
        float destY = 15.0f;
        
        //record timestamp for later verification
        Long timestamp = System.currentTimeMillis();
        
        //setup expected broadcast messages to the game
        EasyMock.resetToDefault(currentGame);
        currentGame.send(null, ServerMessages.createStopMOBPkt(testPlayerId, startX, startY));
        EasyMock.replay(currentGame);
        
        //make the move
        testPlayer.moveMe(newX, newY, destX, destY);
        
        //verify player information has transitioned properly
        verifyState(testPlayer, SnowmanPlayerImpl.PlayerState.STOPPED);
        verifyTimestamp(testPlayer, timestamp);
        verifyLocation(testPlayer, startX, startY);
        verifyDestination(testPlayer, startX, startY);
        
        //verify message has been sent
        EasyMock.verify(currentGame);
    }
    
    
    /**
     * Test the processing of a MOVEME packet when the player is in
     * the moving position and the client sends a start position that is
     * in range
     */
    @Test
    public void testMoveMePlayerMovingAndValidStart()
            throws Exception
    {
        //setup the test players current state
        float startX = 0.0f;
        float startY = 0.0f;
        float destX = 100.0f;
        float destY = 100.0f;
        long startTime = 1000;
        long nowTime = 2000;
        int hp = 100;
        testPlayer.setReadyToPlay(true);
        testPlayer.setLocation(startX, startY);
        this.setup(testPlayer, SnowmanPlayerImpl.PlayerState.MOVING, startTime, destX, destY, hp);
        
        //determine the expected position after 1 second
        float ratePerMs = (EForce.Movement.getMagnitude() / HPConverter.getInstance().convertMass(hp)) * 0.00001f;
        float distanceTraveled = ratePerMs * (nowTime - startTime);
        float offset = (float)Math.sqrt(((distanceTraveled*distanceTraveled)/2.0f));
        float expX = startX + offset;
        float expY = startY + offset;


        //choose a position within the tolerance
        float targetDistanceSqd = SnowmanPlayerImpl.POSITIONTOLERANCESQD/2.0f;
        float xOffset = (float)Math.sqrt(targetDistanceSqd/2.0f);
        float yOffset = (float) Math.sqrt(targetDistanceSqd / 2.0f);
        float newX = expX+xOffset;
        float newY = expY-yOffset;
        
        //setup expected broadcast messages to the game
        EasyMock.resetToDefault(currentGame);
        currentGame.send(null, ServerMessages.createMoveMOBPkt(testPlayerId, newX, newY, destX, destY));
        EasyMock.replay(currentGame);
        
        //make the move
        testPlayer.moveMe(nowTime, newX, newY, destX, destY);
        
        //verify player information has transitioned properly
        verifyState(testPlayer, SnowmanPlayerImpl.PlayerState.MOVING);
        verifyTimestamp(testPlayer, nowTime);
        verifyLocation(testPlayer, newX, newY);
        verifyDestination(testPlayer, destX, destY);
        
        //verify message has been sent
        EasyMock.verify(currentGame);
    }
    
    
    /**
     * Test the processing of a MOVEME packet when the player is in
     * the moving position and the client sends a start position that is
     * out of range
     */
    @Test
    public void testMoveMePlayerMovingAndInvalidStart()
            throws Exception
    {
        //setup the test players current state
        float startX = 0.0f;
        float startY = 0.0f;
        float destX = 100.0f;
        float destY = 100.0f;
        long startTime = 1000;
        long nowTime = 2000;
        int hp = 100;
        testPlayer.setReadyToPlay(true);
        testPlayer.setLocation(startX, startY);
        this.setup(testPlayer, SnowmanPlayerImpl.PlayerState.MOVING, startTime, destX, destY, hp);
        
        //determine the expected position after 1 second
        float ratePerMs = (EForce.Movement.getMagnitude() / HPConverter.getInstance().convertMass(hp)) * 0.00001f;
        float distanceTraveled = ratePerMs * (nowTime - startTime);
        float offset = (float)Math.sqrt(((distanceTraveled*distanceTraveled)/2.0f));
        float expX = startX + offset;
        float expY = startY + offset;


        //choose a position outside the tolerance
        float targetDistanceSqd = SnowmanPlayerImpl.POSITIONTOLERANCESQD*2.0f;
        float xOffset = (float)Math.sqrt(targetDistanceSqd/2.0f);
        float yOffset = (float) Math.sqrt(targetDistanceSqd / 2.0f);
        float newX = expX+xOffset;
        float newY = expY-yOffset;
        
        //setup expected broadcast messages to the game
        EasyMock.resetToDefault(currentGame);
        currentGame.send(null, ServerMessages.createStopMOBPkt(testPlayerId, expX, expY));
        EasyMock.replay(currentGame);
        
        //make the move
        testPlayer.moveMe(nowTime, newX, newY, destX, destY);
        
        //verify player information has transitioned properly
        verifyState(testPlayer, SnowmanPlayerImpl.PlayerState.STOPPED);
        verifyTimestamp(testPlayer, nowTime);
        verifyLocation(testPlayer, expX, expY);
        verifyDestination(testPlayer, expX, expY);
        
        //verify message has been sent
        EasyMock.verify(currentGame);
    }
    
    /**
     * Test the processing of a MOVEME packet when the player is in
     * the dead state
     */
    @Test
    public void testMoveMePlayerDeadAndValidStart()
            throws Exception
    {
        //setup the test players current state
        float startX = 5.0f;
        float startY = 10.0f;
        float destX = 5.0f;
        float destY = 10.0f;
        long startTime = 1000;
        int hp = 0;
        testPlayer.setReadyToPlay(true);
        testPlayer.setLocation(startX, startY);
        this.setup(testPlayer, SnowmanPlayerImpl.PlayerState.DEAD, startTime, destX, destY, hp);

        //choose a position within the tolerance
        float targetDistanceSqd = SnowmanPlayerImpl.POSITIONTOLERANCESQD/2.0f;
        float xOffset = (float)Math.sqrt(targetDistanceSqd/2.0f);
        float yOffset = (float) Math.sqrt(targetDistanceSqd / 2.0f);
        float newX = startX+xOffset;
        float newY = startY-yOffset;
        
        //setup no expected broadcast messages to the game
        EasyMock.resetToDefault(currentGame);
        EasyMock.replay(currentGame);
        
        //make the move
        testPlayer.moveMe(newX, newY, destX, destY);
        
        //verify player information has not changed
        verifyState(testPlayer, SnowmanPlayerImpl.PlayerState.DEAD);
        verifyTimestamp(testPlayer, startTime);
        verifyLocation(testPlayer, startX, startY);
        verifyDestination(testPlayer, startX, startY);
        
        //verify no message has been sent
        EasyMock.verify(currentGame);
    }
    
    /**
     * Test the processing of a MOVEME packet when the player is not
     * in a game
     */
    @Test
    public void testMoveMePlayerNoneAndValidStart()
            throws Exception
    {
        //setup the test players current state
        float startX = 5.0f;
        float startY = 10.0f;
        float destX = 5.0f;
        float destY = 10.0f;
        long startTime = 1000;
        int hp = 0;
        testPlayer.setReadyToPlay(true);
        testPlayer.setLocation(startX, startY);
        this.setup(testPlayer, SnowmanPlayerImpl.PlayerState.NONE, startTime, destX, destY, hp);

        //choose a position within the tolerance
        float targetDistanceSqd = SnowmanPlayerImpl.POSITIONTOLERANCESQD/2.0f;
        float xOffset = (float)Math.sqrt(targetDistanceSqd/2.0f);
        float yOffset = (float) Math.sqrt(targetDistanceSqd / 2.0f);
        float newX = startX+xOffset;
        float newY = startY-yOffset;
        
        //setup no expected broadcast messages to the game
        EasyMock.resetToDefault(currentGame);
        EasyMock.replay(currentGame);
        
        //make the move
        testPlayer.moveMe(newX, newY, destX, destY);
        
        //verify player information has not changed
        verifyState(testPlayer, SnowmanPlayerImpl.PlayerState.NONE);
        verifyTimestamp(testPlayer, startTime);
        verifyLocation(testPlayer, startX, startY);
        verifyDestination(testPlayer, startX, startY);
        
        //verify no message has been sent
        EasyMock.verify(currentGame);
    }
    
    
    /**
     * Verify that attacking with the following conditions works properly:
     *  - attacker is stopped
     *  - attackee is stopped
     *  - client sends valid attack position to server
     *  - attackee is within range
     * @throws java.lang.Exception
     */
    //@Test
    public void testAttackPlayerStopped() 
            throws Exception
    {
        this.initializeAttackee(attackeeId);
        
        //setup the test players current state
        float startX = 5.0f;
        float startY = 10.0f;
        this.setupStoppedPlayer(testPlayer, startX, startY);
        
        //choose an attackee position within the tolerance
        float targetDistanceSqd = SnowmanPlayerImpl.ATTACKTOLERANCESQD/2.0f;
        float xOffset = (float)Math.sqrt(targetDistanceSqd/2.0f);
        float yOffset = (float) Math.sqrt(targetDistanceSqd / 2.0f);
        float attackeeX = startX+xOffset;
        float attackeeY = startY-yOffset;
        
        //setup the attackee state
        this.setupStoppedPlayer(attackee, attackeeX, attackeeY);
        
        //choose an attack position within the tolerance
        targetDistanceSqd = SnowmanPlayerImpl.POSITIONTOLERANCESQD/2.0f;
        xOffset = (float)Math.sqrt(targetDistanceSqd/2.0f);
        yOffset = (float) Math.sqrt(targetDistanceSqd / 2.0f);
        float newX = startX+xOffset;
        float newY = startY-yOffset;
        
        //setup expected broadcast messages to the game and behavior of game
        EasyMock.resetToDefault(currentGame);
        EasyMock.expect(currentGame.getPlayer(attackeeId)).andStubReturn(attackee);
        currentGame.send(null, ServerMessages.createAttackedPkt(testPlayerId, attackeeId, SnowmanPlayerImpl.ATTACKHP));
        EasyMock.replay(currentGame);
        
        //do the attack
        testPlayer.attack(attackeeId, newX, newY);
        
        //verify player state
        this.verifyAttackStop(testPlayer, newX, newY);
        this.verifyAttackHit(attackee, attackeeX, attackeeY, SnowmanPlayerImpl.RESPAWNHP - SnowmanPlayerImpl.ATTACKHP);
        
        //validate messages
        EasyMock.verify(currentGame);
    }
    
    /**
     * Setup the dummy currentGame to return a SnowmanPlayer with the given
     * id intended to be used as the attackee in the attack tests
     */
    private void initializeAttackee(int id) {
        attackeeSession = EasyMock.createNiceMock(ClientSession.class);
        EasyMock.replay(attackeeSession);
        
        attackee = new SnowmanPlayerImpl(appContext, session);
        attackee.setGame(currentGame);
        attackee.setID(attackeeId);
        attackee.setTeamColor(attackeeColor);
    }
    
    /**
     * Setup the player to be in the stopped position at the given timestamp
     */
    private void setupStoppedPlayer(SnowmanPlayerImpl player, float x, float y)
            throws Exception {
       player.setReadyToPlay(true);
       player.setLocation(x, y);
    }
    
    /**
     * Verify that player is stopped in position x, y which means it has initiated
     * an attack from that position
     */
    private void verifyAttackStop(SnowmanPlayerImpl player, float x, float y)
            throws Exception
    {
        verifyState(player, SnowmanPlayerImpl.PlayerState.STOPPED);
        verifyLocation(player, x, y);
        verifyDestination(player, x, y);
    }
    
    /**
     * Verify that player is stopped in position x, y and has the given
     * hit point value
     */
    private void verifyAttackHit(SnowmanPlayerImpl player, float x, float y, int hp)
            throws Exception
    {
        verifyState(player, SnowmanPlayerImpl.PlayerState.STOPPED);
        verifyLocation(player, x, y);
        verifyDestination(player, x, y);
        Assert.assertEquals(player.getHitPoints(), hp);
    }
    
    
    
    /**
     * Verify that the give player has its private state field
     * set to the given state
     */
    private void verifyState(SnowmanPlayerImpl player, SnowmanPlayerImpl.PlayerState state) 
            throws Exception
    {
        Field stateField = SnowmanPlayerImpl.class.getDeclaredField("state");
        stateField.setAccessible(true);
        SnowmanPlayerImpl.PlayerState value = (SnowmanPlayerImpl.PlayerState)stateField.get(player);
        
        Assert.assertEquals(value, state);
    }
    
    /**
     * Verify that the given player has its private timestamp 
     * set to the given value (within 20ms)
     */
    private void verifyTimestamp(SnowmanPlayerImpl player, long timestamp) 
            throws Exception
    {
        Field field = SnowmanPlayerImpl.class.getDeclaredField("timestamp");
        field.setAccessible(true);
        long value = field.getLong(player);
        
        Assert.assertTrue(value - timestamp < 20);
    }
    
    /**
     * Verify that the given player has its private x and y coordinates
     * set to the given coordinates
     */
    private void verifyLocation(SnowmanPlayerImpl player, float x, float y)
            throws Exception
    {
        Field xField = SnowmanPlayerImpl.class.getDeclaredField("startX");
        Field yField = SnowmanPlayerImpl.class.getDeclaredField("startY");
        xField.setAccessible(true);
        yField.setAccessible(true);
        float xValue = xField.getFloat(player);
        float yValue = yField.getFloat(player);
        
        Assert.assertEquals(xValue, x);
        Assert.assertEquals(yValue, y);
    }
    
    /**
     * Verify that the given player has its private destination x and y coordinates
     * set to the given coordinates
     */
    private void verifyDestination(SnowmanPlayerImpl player, float x, float y)
            throws Exception
    {
        Field xField = SnowmanPlayerImpl.class.getDeclaredField("destX");
        Field yField = SnowmanPlayerImpl.class.getDeclaredField("destY");
        xField.setAccessible(true);
        yField.setAccessible(true);
        float xValue = xField.getFloat(player);
        float yValue = yField.getFloat(player);
        
        Assert.assertEquals(xValue, x);
        Assert.assertEquals(yValue, y);
    }
    
    private void setup(SnowmanPlayerImpl player,
                       SnowmanPlayerImpl.PlayerState state,
                       long timestamp,
                       float destX,
                       float destY,
                       int hp)
            throws Exception
    {
        Field stateField = SnowmanPlayerImpl.class.getDeclaredField("state");
        Field timeField = SnowmanPlayerImpl.class.getDeclaredField("timestamp");
        Field xField = SnowmanPlayerImpl.class.getDeclaredField("destX");
        Field yField = SnowmanPlayerImpl.class.getDeclaredField("destY");
        Field hpField = SnowmanPlayerImpl.class.getDeclaredField("hitPoints");
        
        stateField.setAccessible(true);
        timeField.setAccessible(true);
        xField.setAccessible(true);
        yField.setAccessible(true);
        hpField.setAccessible(true);
        
        stateField.set(player, state);
        timeField.setLong(player, timestamp);
        xField.setFloat(player, destX);
        yField.setFloat(player, destY);
        hpField.setInt(player, hp);
    }

}
