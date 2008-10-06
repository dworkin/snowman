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
package com.sun.darkstar.example.snowman.game.input.enumn;

/**
 * <code>EInputConverter</code> defines the enumerations of all types of GUI
 * <code>IInputConverter</code>.
 * 
 * @author Yi Wang (Neakor)
 * @version Creation date: 07-16-2008 10:52 EST
 * @version Modified date: 07-21-2008 12:03 EST
 */
public enum EInputConverter {
	/**
	 * The keyboard GUI input converter enumeration.
	 */
	KeyboardConverter(EInputType.Keyboard),
	/**
	 * The mouse GUI input converter enumeration.
	 */
	MouseConverter(EInputType.Mouse);
	
	/**
	 * The <code>EInputType</code> enumeration.
	 */
	private final EInputType type;
	
	/**
	 * Constructor of <code>EInputConverter</code>.
	 * @param type The <code>EInputType</code> enumeration.
	 */
	private EInputConverter(EInputType type) {
		this.type = type;
	}
	
	/**
	 * Retrieve the input type of this converter type.
	 * @return The <code>EInputType</code> enumeration.
	 */
	public EInputType getType() {
		return this.type;
	}
}
