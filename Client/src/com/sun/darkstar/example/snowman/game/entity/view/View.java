package com.sun.darkstar.example.snowman.game.entity.view;

import java.io.IOException;

import com.jme.scene.Node;
import com.jme.scene.TriMesh;
import com.jme.util.export.InputCapsule;
import com.jme.util.export.JMEExporter;
import com.jme.util.export.JMEImporter;
import com.jme.util.export.OutputCapsule;
import com.sun.darkstar.example.snowman.interfaces.IEntity;
import com.sun.darkstar.example.snowman.interfaces.IView;

/**
 * <code>View</code> defines the representation of an <code>IEntity</code>.
 * It is a subclass of <code>Node</code> which makes it possible to directly
 * attach <code>View</code> to the scene graph for rendering.
 * <p>
 * <code>View</code> only defines an abstraction for its subclasses that
 * represent specific types of views.
 * <p>
 * <code>View</code> overrides the binary import and export methods of its
 * parent class to allow import and export of its own variable fields.
 * 
 * @author Yi Wang (Neakor)
 * @version Creation date: 06-05-2008 14:44 EST
 * @version Modified date: 07-07-2008 13:31 EST
 */
public abstract class View extends Node implements IView {
	/**
	 * Serial version.
	 */
	private static final long serialVersionUID = 3285825999307706598L;
	/**
	 * The <code>IEntity</code> this <code>View</code> represents.
	 */
	private IEntity entity;
	
	/**
	 * Constructor of <code>View</code>.
	 */
	public View() {
		super();
	}
	
	/**
	 * Constructor of <code>View</code>.
	 * @param entity The <code>IEntity</code> this view represents.
	 */
	public View(IEntity entity) {
		super(entity.getEnumn().toString()+"_View");
		this.entity = entity;
		this.setCullHint(CullHint.Dynamic);
	}
	
	@Override
	public void attachMesh(TriMesh mesh) {
		this.attachChild(mesh);
	}

	@Override
	public void attachTo(Node parent) {
		parent.attachChild(this);
	}

	@Override
	public boolean detachFromParent() {
		return this.removeFromParent();
	}
	
	@Override
	public boolean equals(Object object) {
		if(object instanceof IView) {
			IView given = (IView)object;
			return given.getEntity().equals(this.entity);
		}
		return false;
	}

	@Override
	public IEntity getEntity() {
		return this.entity;
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public Class getClassTag() {
		return View.class;
	}
	
	@Override
	public void write (JMEExporter ex) throws IOException {
		super.write(ex);
		OutputCapsule oc = ex.getCapsule(this);
		oc.write(this.entity, "Entity", null);
	}
	
	@Override
	public void read(JMEImporter im) throws IOException {
		super.read(im);
		InputCapsule ic = im.getCapsule(this);
		this.entity = (IEntity)ic.readSavable("Entity", null);
	}
}