package ar.com.tamborindeguy.client.systems.render.world;

import ar.com.tamborindeguy.client.handlers.DescriptorHandler;
import ar.com.tamborindeguy.client.handlers.ParticlesHandler;
import ar.com.tamborindeguy.client.systems.camera.CameraSystem;
import ar.com.tamborindeguy.model.descriptors.BodyDescriptor;
import ar.com.tamborindeguy.model.descriptors.FXDescriptor;
import ar.com.tamborindeguy.model.map.Tile;
import ar.com.tamborindeguy.model.textures.BundledAnimation;
import ar.com.tamborindeguy.util.Util;
import com.artemis.Aspect;
import com.artemis.E;
import com.artemis.annotations.Wire;
import com.artemis.systems.IteratingSystem;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.esotericsoftware.minlog.Log;
import entity.Body;
import graphics.FX;
import position.Pos2D;
import position.WorldPos;

import java.util.*;

import static com.artemis.E.E;

@Wire
public class FXsRenderingSystem extends IteratingSystem {

    private SpriteBatch batch;

    private CameraSystem cameraSystem;

    private Map<Integer, Map<Integer, BundledAnimation>> fxs = new HashMap<>();
    private int srcFunc;
    private int dstFunc;

    public FXsRenderingSystem(SpriteBatch batch) {
        super(Aspect.all(FX.class, WorldPos.class));
        this.batch = batch;
    }

    @Override
    protected void begin() {
        cameraSystem.camera.update();
        batch.setProjectionMatrix(cameraSystem.camera.combined);
        // remember SpriteBatch's current functions
        srcFunc = batch.getBlendSrcFunc();
        dstFunc = batch.getBlendDstFunc();
        batch.enableBlending();
        batch.begin();
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_DST_ALPHA);
    }

    @Override
    protected void end() {
        batch.end();
        batch.setBlendFunction(srcFunc, dstFunc);
    }

    @Override
    protected void removed(int entityId) {
        super.removed(entityId);
        fxs.remove(entityId);
    }

    @Override
    protected void process(int entityId) {
        E entity = E(entityId);
        Pos2D screenPos = Util.toScreen(entity.worldPosPos2D());
        final FX fx = entity.getFX();
        if (fx.fxs.isEmpty()) {
            return;
        }
        List<Integer> removeFXs = new ArrayList<>();
        drawFXs(entityId, screenPos, fx, removeFXs);
        removeFXs.forEach(remove -> fx.removeFx(remove));
        if (fx.fxs.isEmpty()) {
            fxs.remove(entityId);
        }
    }

    private void drawFXs(int entityId, Pos2D screenPos, FX fx, List<Integer> removeFXs) {
        if (fx == null || fx.fxs == null) {
            return;
        }
        fx.fxs.forEach(fxId -> {
            FXDescriptor fxDescriptor = DescriptorHandler.getFX(fxId);
            Map<Integer, BundledAnimation> anims = this.fxs.computeIfAbsent(entityId, id -> new HashMap<>());
            int bodyOffset = getBodyOffset(entityId);
            BundledAnimation anim = anims.computeIfAbsent(fxId, fxGraphic -> new BundledAnimation(DescriptorHandler.getGraphic(fxDescriptor.getIndexs()[0])));
            TextureRegion graphic = anim.getGraphic(false);
            batch.draw(graphic, screenPos.x - (Tile.TILE_PIXEL_WIDTH + graphic.getRegionWidth()) / 2 + fxDescriptor.getOffsetX(), screenPos.y - graphic.getRegionHeight() + fxDescriptor.getOffsetY() + bodyOffset);
            anim.setAnimationTime(anim.getAnimationTime() + getWorld().getDelta() * (anim.getFrames().size * 0.33f));
            if (anim.isAnimationFinished()) {
                removeFXs.add(fxId);
                anims.remove(fxId);
            }
        });
    }

    private int getBodyOffset(int entityId) {
        int headOffsetY = 0;
        if (E(entityId).hasBody()) {
            final Body body = E(entityId).getBody();
            BodyDescriptor bodyDescriptor = DescriptorHandler.getBodies().get(body.index);
            headOffsetY = Math.max(0, bodyDescriptor.getHeadOffsetY());
        }
        return headOffsetY;
    }

}
