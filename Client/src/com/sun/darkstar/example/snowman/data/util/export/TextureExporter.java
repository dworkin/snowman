package com.sun.darkstar.example.snowman.data.util.export;

import java.net.URL;

import com.jme.bounding.BoundingBox;
import com.jme.image.Texture;
import com.jme.input.KeyBindingManager;
import com.jme.scene.shape.Quad;
import com.jme.scene.state.BlendState;
import com.jme.scene.state.TextureState;
import com.jme.util.TextureManager;

/**
 * <code>TextureExporter</code> exports image files into <code>Texture</code>
 * binary files.
 * 
 * @author Yi Wang (Neakor)
 * @version Creation date: 06-13-2008 10:47 EST
 * @version Modified date: 06-20-2008 14:28 EST
 */
public class TextureExporter extends Exporter {
	/**
	 * The source image file name with extension.
	 */
	private final String inputFile = "UpperDayCloud.png";
	/**
	 * The output binary file with extension.
	 */
	private final String outputFile = "UpperDayCloud.tex";
	/**
	 * The <code>AlphaState</code> for transparency.
	 */
	private BlendState alpha;
	/**
	 * The <code>Texture</code> instance.
	 */
	private Texture texture;

	/**
	 * Main method.
	 */
	public static void main(String[] args) {
		new TextureExporter().start();
	}
	
	/**
	 * Constructor of <code>TextureExporter</code>.
	 */
	public TextureExporter() {
		super("com/sun/darkstar/example/snowman/data/texture/");
		TextureManager.COMPRESS_BY_DEFAULT = false;
	}

	@Override
	protected void initialize() {
		this.setupBlend();
		this.buildQuad();
	}

	/**
	 * Setup the <code>BlendState</code> for transparency.
	 */
	private void setupBlend() {
		this.alpha = this.display.getRenderer().createBlendState();
		this.alpha.setBlendEnabled(true);
		this.alpha.setSourceFunction(BlendState.SourceFunction.SourceAlpha);
		this.alpha.setDestinationFunction(BlendState.DestinationFunction.OneMinusSourceAlpha);
		this.alpha.setTestEnabled(true);
		this.alpha.setTestFunction(BlendState.TestFunction.GreaterThan);
		this.alpha.setEnabled(true);
	}
	
	/**
	 * Build a <code>Quad</code> to preview the <code>Texture</code>.
	 */
	private void buildQuad() {
		Quad q = new Quad("Export", 20,	20);
		q.setModelBound(new BoundingBox());
		q.updateModelBound();
		q.setRenderState(this.alpha);
		URL url = this.getClass().getClassLoader().getResource(this.sourceDir + this.inputFile);
		this.texture = TextureManager.loadTexture(url, Texture.MinificationFilter.Trilinear, Texture.MagnificationFilter.Bilinear, 16, true);
		TextureState ts = this.display.getRenderer().createTextureState();
		ts.setTexture(this.texture);
		q.setRenderState(ts);
		this.rootNode.attachChild(q);
	}

	@Override
	protected void simpleUpdate() {
		if(KeyBindingManager.getKeyBindingManager().isValidCommand("out", false)) {
			this.export(this.outputFile, this.texture);
		}
	}
}