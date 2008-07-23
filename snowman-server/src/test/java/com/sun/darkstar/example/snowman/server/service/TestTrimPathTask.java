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

package com.sun.darkstar.example.snowman.server.service;

import com.sun.darkstar.example.snowman.common.util.CollisionManager;
import com.jme.scene.Spatial;
import com.jme.scene.Node;
import com.jme.math.Vector3f;
import org.junit.Test;
import org.easymock.EasyMock;

/**
 *
 * @author Owen Kellett
 */
public class TestTrimPathTask 
{
    /**
     * Verify that running the task notifies the callback with
     * the X and Z coordinates of the resulting Vector3f from the
     * CollisionManager
     */
    @Test
    public void testRun() throws Exception {
        //prepare test data
        int playerId = 1;
        float startx = 1.0f;
        float starty = 2.0f;
        float endx = 10.0f;
        float endy = 11.0f;
        long timestart = 123l;
        float realEndx = 5.0f;
        float realEndy = 6.0f;
        Vector3f dummyDestination = new Vector3f(realEndx, 100.0f, realEndy);
        Spatial dummyWorld = new Node();
        
        //prepare the mock CollisionManager
        CollisionManager mockCollisionManager = EasyMock.createMock(CollisionManager.class);
        EasyMock.expect(mockCollisionManager.getDestination(startx, starty, endx, endy, dummyWorld)).andReturn(dummyDestination);
        EasyMock.replay(mockCollisionManager);
        
        //prepare the callback interface
        GameWorldServiceCallback mockCallback = EasyMock.createMock(GameWorldServiceCallback.class);
        mockCallback.trimPathComplete(playerId, startx, starty, realEndx, realEndy, timestart);
        EasyMock.replay(mockCallback);
        
        //make the actual run call
        TrimPathTask task = new TrimPathTask(playerId,
                                             startx,
                                             starty,
                                             endx,
                                             endy,
                                             timestart,
                                             mockCallback,
                                             dummyWorld,
                                             mockCollisionManager);
        task.run();
        
        //verify callback called
        EasyMock.verify(mockCallback);
    }

}