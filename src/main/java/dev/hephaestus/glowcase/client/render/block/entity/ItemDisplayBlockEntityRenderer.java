package dev.hephaestus.glowcase.client.render.block.entity;

import dev.hephaestus.glowcase.block.entity.ItemDisplayBlockEntity;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3f;

public record ItemDisplayBlockEntityRenderer(BlockEntityRendererFactory.Context context) implements BlockEntityRenderer<ItemDisplayBlockEntity> {
	private static final MinecraftClient mc = MinecraftClient.getInstance();

	@Override
	public void render(ItemDisplayBlockEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
		PlayerEntity player = mc.player;

		if (player == null) return;

		matrices.push();
		matrices.translate(0.5D, 0D, 0.5D);

		float yaw = 0F;
		float pitch = 0F;

		switch (entity.rotationType) {
			case TRACKING -> {
				Vec2f pitchAndYaw = ItemDisplayBlockEntity.getPitchAndYaw(player, entity.getPos());
				pitch = pitchAndYaw.x;
				yaw = pitchAndYaw.y;
				matrices.multiply(Vec3f.POSITIVE_Y.getRadialQuaternion(yaw));
			}
			case HORIZONTAL -> {
				float rotation = -(entity.getCachedState().get(Properties.ROTATION) * 360) / 16.0F;
				matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(rotation));
			}
			case LOCKED -> {
				pitch = entity.pitch;
				yaw = entity.yaw;
				matrices.multiply(Vec3f.POSITIVE_Y.getRadialQuaternion(yaw));
			}
		}

		ItemStack stack = entity.getUseStack();
		Text name;
		if (stack.getItem() instanceof SpawnEggItem) {
			matrices.push();
			Entity renderEntity = entity.getDisplayEntity();
			if (renderEntity != null) {
				if (stack.hasCustomName()) {
					name = stack.getName();
				} else {
					name = renderEntity.getName();
				}

				float scale = renderEntity.getHeight() > renderEntity.getWidth() ? 1F / renderEntity.getHeight() : 0.5F;
				matrices.scale(scale, scale, scale);

				renderEntity.setPitch(-pitch * 57.2957763671875F);
				renderEntity.setHeadYaw(yaw);

				matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(180));
				EntityRenderer<? super Entity> entityRenderer = context.getEntityRenderDispatcher().getRenderer(renderEntity);
				entityRenderer.render(renderEntity, 0, tickDelta, matrices, vertexConsumers, light);
			} else {
				name = Text.empty();
			}
			matrices.pop();
			matrices.translate(0, 0.125F, 0);
			matrices.scale(0.5F, 0.5F, 0.5F);
		} else {
			name = stack.isEmpty() ? Text.translatable("gui.glowcase.none") : (Text.literal("")).append(stack.getName()).formatted(stack.getRarity().formatting);
			matrices.translate(0, 0.5, 0);
			matrices.scale(0.5F, 0.5F, 0.5F);
			matrices.multiply(Vec3f.POSITIVE_X.getRadialQuaternion(pitch));
			context.getItemRenderer().renderItem(entity.getUseStack(), ModelTransformation.Mode.FIXED, light, OverlayTexture.DEFAULT_UV, matrices, vertexConsumers, 0);
		}

		if (entity.showName) {
			HitResult hitResult = mc.crosshairTarget;
			if (hitResult instanceof BlockHitResult && ((BlockHitResult) hitResult).getBlockPos().equals(entity.getPos())) {
				matrices.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(180.0F));
				matrices.translate(0, 0, -0.4);

				float scale = 0.025F;
				matrices.scale(scale, scale, scale);

				int color = name.getStyle().getColor() == null ? 0xFFFFFF : name.getStyle().getColor().getRgb();
				matrices.translate(-context.getTextRenderer().getWidth(name) / 2F, -4, 0);
				context.getTextRenderer().drawWithShadow(matrices, name, 0, 0, color);
			}
		}

		matrices.pop();
	}
}
