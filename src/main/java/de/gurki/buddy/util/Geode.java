package de.gurki.buddy.util;

import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import org.apache.commons.lang3.ObjectUtils.Null;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.AbstractIterator;

import java.util.Optional;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;


public class Geode
{
    public static final String MOD_ID = "de.gurki.buddy";
	public static final Logger LOGGER = LogManager.getLogger( MOD_ID );

    public BlockBox box_ = null;


    ////////////////////////////////////////////////////////////////////////////////
    public Geode() {}


    ////////////////////////////////////////////////////////////////////////////////
    public boolean isEmpty() {
        return box_ == null;
    }


    ////////////////////////////////////////////////////////////////////////////////
    public BlockPos min() {
        return new BlockPos( box_.getMinX(), box_.getMinY(), box_.getMinZ() );
    }


    ////////////////////////////////////////////////////////////////////////////////
    public BlockPos max() {
        return new BlockPos( box_.getMaxX(), box_.getMaxY(), box_.getMaxZ() );
    }


    ////////////////////////////////////////////////////////////////////////////////
    public void setHighlight( boolean show, World world )
    {
        BlockPos pos = min().add( -1, -1, -1 );

        if ( ! show ) {
            world.setBlockState( pos, Blocks.AIR.getDefaultState() );
            return;
        }

        if ( world.getBlockState( pos ).isOf( Blocks.STRUCTURE_BLOCK ) ) {
            return;
        }

        BlockBox box = BlockBox.create( min(), max() );
        Utility.highlightBox( box, world );
    }


    ////////////////////////////////////////////////////////////////////////////////
    public void clear( World world ) {
        setHighlight( false, world );
        box_ = null;
    }


    ////////////////////////////////////////////////////////////////////////////////
    public void addClosest( BlockPos center, World world, int range )
    {
        BlockBox newBox = findClosest( center, world, range );

        if ( newBox == null ) {
            return;
        }

        if ( box_ == null ) {
            box_ = newBox;
        } else {
            setHighlight( false, world );
            box_ = BlockBox.encompass( List.of( newBox, box_ ) ).get();
        }

        setHighlight( true, world );
    }


    ////////////////////////////////////////////////////////////////////////////////
    public static BlockBox findClosest( BlockPos center, World world, int range )
    {
        // LOGGER.info( "expanding bounding box..." );

        //  seed

        final int searchRadius = Constants.kSearchRadius;

        Optional<BlockPos> maybePos = BlockPos
            .streamOutwards( center, searchRadius, searchRadius, searchRadius )
            .filter( i -> world.getBlockState( i ).isOf( Blocks.BUDDING_AMETHYST ) )
            .findFirst();

        if ( maybePos.isEmpty() ) {
            return null;
        }

        //  expand

        BlockBox box = new BlockBox( maybePos.get() );
        int count = 0;
        boolean foundBuds = true;

        while ( foundBuds )
        {
            foundBuds = false;

            for ( int offset = 1; offset <= range; offset++ )
            {
                // LOGGER.info( box );
                List<BlockPos> buds = streamBoxSurface( box, offset )
                    .filter( i -> world.getBlockState( i ).isOf( Blocks.BUDDING_AMETHYST ) )
                    .toList();

                if ( buds.isEmpty() ) {
                    continue;
                }

                foundBuds = true;
                count += buds.size();

                BlockBox newBox = BlockBox.encompassPositions( buds ).get();
                box = BlockBox.encompass( List.of( newBox, box ) ).get();

                break;
            }
        }

        if ( count == 0 ) {
            return null;
        }

        return box;
    }


    ////////////////////////////////////////////////////////////////////////////////
    public static Iterable<BlockPos> iterateBoxSurface( BlockBox box, int offset )
    {
        return () -> new AbstractIterator<BlockPos>()
        {
            private static final int[] dirIds = { 2, 5, 3, 4, 0, 1 };
            private final int[] dirDims = { box.getMinY(), box.getMaxY(), box.getMinZ(), box.getMaxZ(), box.getMinX(), box.getMaxX() };

            private int stageId = -1;
            private int u = 0;
            private int v = 0;
            private BlockPos corner = null;
            private Direction dir = null;
            private Direction dirU = null;
            private Direction dirV = null;
            private int minU = 0;
            private int minV = 0;
            private int maxU = 0;
            private int maxV = 0;
            {
                // LOGGER.info( box );
                // LOGGER.info( List.of( dirDims ) );
                advanceStage();
            }

            protected void advanceStage()
            {
                this.stageId++;

                if ( this.stageId > 5 ) {
                    return;
                }

                this.dir = Direction.byId( dirIds[ this.stageId ] );

                if ( this.stageId < 4 ) {
                    this.dirU = Direction.byId( dirIds[ ( this.stageId + 1 ) % 4 ] );
                    this.dirV = Direction.UP;
                }
                else {
                    this.dirU = Direction.EAST;
                    this.dirV = Direction.SOUTH;
                }

                this.corner = new BlockPos( 0, 0, 0 )
                    .offset( this.dirU.getAxis(), dirDims[ this.dirU.getOpposite().getId() ] )
                    .offset( this.dirV.getAxis(), dirDims[ this.dirV.getOpposite().getId() ] )
                    .offset( this.dir.getAxis(), dirDims[ this.dir.getId() ] )
                    .offset( this.dir, offset );

                this.minU = -offset + 1;
                this.minV = -offset;
                this.maxU = Math.abs( dirDims[ this.dirU.getId() ] - corner.getComponentAlongAxis( this.dirU.getAxis() ) ) + offset;
                this.maxV = Math.abs( dirDims[ this.dirV.getId() ] - corner.getComponentAlongAxis( this.dirV.getAxis() ) ) + offset;

                if ( this.stageId >= 4 ) {
                    this.minV++;
                    this.maxU--;
                    this.maxV--;
                }

                this.u = this.minU;
                this.v = this.minV;

                // LOGGER.info( "advanceStage" );
                // LOGGER.info( this.stageId );
                // LOGGER.info( this.dir + " " + this.dirU  + " " + this.dirV );
                // LOGGER.info( this.corner );
                // LOGGER.info( this.minU + " " + this.minV );
                // LOGGER.info( this.maxU + " " + this.maxV );
                // LOGGER.info( this.u + " " + this.v );
            }

            @Override
            protected BlockPos computeNext()
            {
                if ( this.stageId > 5 ) {
                    return (BlockPos)this.endOfData();
                }

                BlockPos p = this.corner.offset( this.dirU, this.u );
                p = p.offset( this.dirV, this.v );

                if ( this.u < this.maxU ) {
                    this.u++;
                }
                else
                {
                    this.u = this.minU;

                    if ( this.v < this.maxV ) {
                        this.v++;
                    } else {
                        advanceStage();
                    }
                }

                return p;
            }
        };
    }


    ////////////////////////////////////////////////////////////////////////////////
    public static Stream<BlockPos> streamBoxSurface( BlockBox box, int offset ) {
        return StreamSupport.stream( Geode.iterateBoxSurface( box, offset ).spliterator(), false );
    }
}
