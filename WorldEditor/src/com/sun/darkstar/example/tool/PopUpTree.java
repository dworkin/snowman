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
package com.sun.darkstar.example.tool;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.MenuElement;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

/**
 * This super class of JTree creates a tree with a popup menu
 * @author Jeffrey Kesselman
 */
class PopUpTree extends JTree {
    JPopupMenu popup;
    JMenuItem mi;
    
    public PopUpTree(TreeModel model) {
        super(model);
        
        popup = new JPopupMenu();
        //popup.add(new JMenuItem("Default Popup Menu"));
        popup.setOpaque(true);
        popup.setLightWeightPopupEnabled(false);
        addMouseListener(
                new MouseAdapter() {
            @Override
            public void mouseReleased( MouseEvent e ) {
                if ( e.isPopupTrigger()) {
                    TreePath clickedElement = 
                            getPathForLocation (e.getX(),e.getY());
                    setSelectionPath(clickedElement);        
                    popup.show( (JComponent)e.getSource(), e.getX(), e.getY() );
                }
            }
        }
        );
        
    }
    
  /**
   * This method is called to add an item to the tree's popup menu
   * @param item The menu item to add
   */
    public void addToPopup(JMenuItem item){
        popup.add(item);
    }
    
    private int findItemRow(JMenuItem item){
        int i=0;
        for (MenuElement el : popup.getSubElements()){
            if (el == item){
                return i;
            } else {
                i++;
            }
        }
        return -1;
    }
    
}