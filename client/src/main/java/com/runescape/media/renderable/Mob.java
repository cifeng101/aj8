package com.runescape.media.renderable;

import com.runescape.cache.def.MobDefinition;
import com.runescape.cache.media.AnimationSequence;
import com.runescape.cache.media.SpotAnimation;
import com.runescape.media.Animation;

public class Mob extends GameCharacter {

	public MobDefinition npcDefinition;

	private final Model getChildModel() {
		if (animation >= 0 && aniomationDelay == 0) {
			int frameId = AnimationSequence.cache[animation].primaryFrames[anInt1547];
			int frameId2 = -1;
			if (anInt1537 >= 0 && anInt1537 != standAnimationId) {
				frameId2 = AnimationSequence.cache[anInt1537].primaryFrames[anInt1538];
			}
			return npcDefinition.getChildModel(frameId2, frameId, AnimationSequence.cache[animation].interleaveOrder);
		}
		int i_3_ = -1;
		if (anInt1537 >= 0) {
			i_3_ = AnimationSequence.cache[anInt1537].primaryFrames[anInt1538];
		}
		return npcDefinition.getChildModel(-1, i_3_, null);
	}

	@Override
	public final Model getRotatedModel() {
		if (npcDefinition == null) {
			return null;
		}
		Model model = getChildModel();
		if (model == null) {
			return null;
		}
		modelHeight = model.modelHeight;
		if (spotAnimationId != -1 && currentAnimationFrame != -1) {
			SpotAnimation spotanimation = SpotAnimation.cache[spotAnimationId];
			Model model_4_ = spotanimation.getModel();
			if (model_4_ != null) {
				int animationId = spotanimation.sequences.primaryFrames[currentAnimationFrame];
				Model animationModel = new Model(true, Animation.exists(animationId), false, model_4_);
				animationModel.translate(0, -spotAnimationDelay, 0);
				animationModel.createBones();
				animationModel.applyTransform(animationId);
				animationModel.triangleSkin = null;
				animationModel.vectorSkin = null;
				if (spotanimation.resizeXY != 128 || spotanimation.resizeZ != 128) {
					animationModel.scaleT(spotanimation.resizeXY, spotanimation.resizeXY, spotanimation.resizeZ);
				}
				animationModel.applyLighting(64 + spotanimation.modelLightFalloff, 850 + spotanimation.modelLightAmbient, -30, -50, -30, true);
				Model[] models = { model, animationModel };
				model = new Model(2, -819, true, models);
			}
		}
		if (npcDefinition.boundaryDimension == 1) {
			model.oneSquareModel = true;
		}
		return model;
	}

	@Override
	public final boolean isVisibile() {
		if (npcDefinition == null) {
			return false;
		}
		return true;
	}
}
