package org.teacon.exhibition_portal.client.interaction;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.fml.loading.FMLEnvironment;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.teacon.exhibition_portal.ExhibitionPortal;
import org.teacon.exhibition_portal.client.EPClient;
import org.teacon.exhibition_portal.client.framework.binding.RenderAccess;
import org.teacon.exhibition_portal.client.framework.components.Rectangle;
import org.teacon.exhibition_portal.client.screens.stamp.StampScreen;
import org.teacon.exhibition_portal.client.screens.stamp.StampScreenLayouts;
import org.teacon.exhibition_portal.components.EPServer;
import org.teacon.exhibition_portal.components.Exhibition;
import org.teacon.exhibition_portal.components.ExhibitionStamp;

import java.util.List;
import java.util.Map;

public class StampingCounterBlock extends Block implements EntityBlock {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;

    private static final Map<Direction, VoxelShape> SHAPES = Shapes.rotateHorizontal(
            Shapes.or(
                    Block.column(16.0, 0.0, 2.0),
                    Block.column(8.0, 2.0, 11.0),
                    Block.boxZ(16.0, 11.0, 15.0, 2.0, 15.0),
                    Block.box(2.0, 15.0, 11.0, 4.0, 19.0, 13)
            )
    );

    public StampingCounterBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected @NonNull BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @SuppressWarnings("deprecation")
    @Override
    protected @NonNull BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected @NonNull VoxelShape getShape(BlockState state, @NonNull BlockGetter level, @NonNull BlockPos pos, @NonNull CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NonNull BlockPos worldPosition, @NonNull BlockState blockState) {
        return new StampingCounterBlockEntity(worldPosition, blockState);
    }

    @Override
    protected @NonNull InteractionResult useWithoutItem(
            @NonNull BlockState state,
            @NonNull Level level,
            @NonNull BlockPos pos,
            @NonNull Player player,
            @NonNull BlockHitResult hitResult
    ) {
        StampingCounterBlockEntity entity = level.getBlockEntity(pos, ExhibitionPortal.STAMPING_COUNTER_BE.get()).orElse(null);
        if (entity != null && entity.getExhibition() != null && entity.getStampID() != null && entity.getItem() != null) {
            ExhibitionStamp stamp = new ExhibitionStamp(entity.getStampID(), entity.getItem(), new Rectangle(0.4f, 0.4f, 0.2f, 0.2f), 0);
            if (level.isClientSide()) {
                Exhibition exhibition = RenderAccess.get(EPClient.GALLERY_LOOKUP).get(entity.getExhibition());
                if (exhibition != null) {
                    for (ExhibitionStamp s : exhibition.footprint().stamps()) {
                        if (s.id().equals(stamp.id())) {
                            stamp = s;
                            break;
                        }
                    }

                    StampScreenLayouts.setEditingExhibition(exhibition, stamp);
                    // DONOT EDIT: explicit upper cast to prevent loading client-only class n.m.c.g.Screen on dedicated servers
                    Minecraft.getInstance().setScreen((Screen) (Object) new StampScreen());
                }
            } else {
                List<ExhibitionStamp> stamps = EPServer.getFootprint((ServerPlayer) player, entity.getExhibition()).stamps();
                locate:
                {
                    for (ExhibitionStamp s : stamps) {
                        if (s.id().equals(stamp.id())) {
                            break locate;
                        }
                    }

                    ExhibitionStamp s = stamp;
                    EPServer.updateFootprint((ServerPlayer) player, entity.getExhibition(), p -> p.withStamp(s));
                }
            }
        }

        return InteractionResult.SUCCESS;
    }
}
