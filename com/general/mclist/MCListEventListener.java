/*
 * MCList.java
 * 
 * Title: Multi Column List Package Author: Andrew Trumper Description: A
 * MultiColumn List Package
 * 
 * Copyright Andrew Trumper 2001
 */

package com.general.mclist;

public interface MCListEventListener {
    public void doubleClick(MCListEvent e);

    public void selectItem(MCListEvent e);

    public void unselectItem(MCListEvent e);
}