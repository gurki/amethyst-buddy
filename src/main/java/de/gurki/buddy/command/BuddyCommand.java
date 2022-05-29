package de.gurki.buddy.command;

import de.gurki.buddy.util.Graph;

import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import net.minecraft.util.math.BlockPos;
// import net.minecraft.util.math.Direction.Axis;
// import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
// import net.minecraft.predicate.NumberRange.IntRange;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.text.LiteralText;
import net.minecraft.util.math.Direction;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.gurki.buddy.util.Clusters;
import de.gurki.buddy.util.Constants;
import de.gurki.buddy.util.Utility;

import java.util.List;
import java.util.Map;
// import java.lang.reflect.Array;
import java.util.ArrayList;
// import java.util.Collection;
// import java.util.Set;
import java.util.stream.IntStream;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;


public class BuddyCommand
{
    public static final String MOD_ID = "de.gurki.buddy";
	public static final Logger LOGGER = LogManager.getLogger( MOD_ID );

    public static BlockPos center_ = null;
    public static BlockPos boxMin_ = null;
    public static BlockPos boxMax_ = null;


    ////////////////////////////////////////////////////////////////////////////////
    public static void register( CommandDispatcher<ServerCommandSource> dispatcher, boolean dedicated )
    {
        dispatcher.register(
            CommandManager.literal( "buddy" )
                .executes( context -> run( context, 0 ) )
                .then( CommandManager.literal( "test" ).executes( BuddyCommand::test ))
                .then( CommandManager.literal( "init" ).executes( BuddyCommand::init ))
                .then( CommandManager.literal( "tp" ).executes( BuddyCommand::tp ))
                .then( CommandManager.literal( "fly" ).executes( BuddyCommand::fly ))
                .then( CommandManager.literal( "project" ).executes( context -> run( context, 1 ) ))
                .then( CommandManager.literal( "cluster" ).executes( context -> run( context, 2 ) ))
                .then( CommandManager.literal( "connect" ).executes( context -> run( context, 3 ) ))
                .then( CommandManager.literal( "validate" ).executes( context -> run( context, 4 ) ))
                .then( CommandManager.literal( "merge" ).executes( context -> run( context, 5 ) ))
                .then( CommandManager.literal( "split" ).executes( context -> run( context, 6 ) ))
                .then( CommandManager.literal( "explore" ).executes( context -> run( context, -1 ) ))
        );
    }


    ////////////////////////////////////////////////////////////////////////////////
    public static int tp( CommandContext<ServerCommandSource> context ) throws CommandSyntaxException {
        context.getSource().getPlayer().teleport( context.getSource().getWorld(), -734, 14.5, -254, 90, 0 );
        return 1;
    }


    ////////////////////////////////////////////////////////////////////////////////
    public static int test( CommandContext<ServerCommandSource> context ) throws CommandSyntaxException
    {
        HashSet<BlockPos> blocks = new HashSet<>();
        blocks.add( new BlockPos( 0, 0, 0 ) );
        blocks.add( new BlockPos( 1, 0, 0 ) );
        blocks.add( new BlockPos( 2, 0, 0 ) );
        blocks.add( new BlockPos( 1, 1, 0 ) );

        Graph graph = new Graph( blocks );

        LOGGER.info( List.of( graph.verts ) );
        LOGGER.info( graph.edges );
        LOGGER.info( "iterateBFS" );

        Iterator<Integer> iter = graph.iterateBFS( 0, i -> i % 2 == 0 ).iterator();

        while ( iter.hasNext() ) {
            Integer v = iter.next();
            LOGGER.info( "vert: " + v );
        }

        return 1;
    }


    ////////////////////////////////////////////////////////////////////////////////
    public static int fly( CommandContext<ServerCommandSource> context ) throws CommandSyntaxException
    {
        ServerPlayerEntity player = context.getSource().getPlayer();
        BlockPos center = player.getBlockPos();
        Direction dir = player.getHorizontalFacing();
        Utility.buildMachine( center, dir.rotateYCounterclockwise(), Direction.UP, Blocks.SLIME_BLOCK, context.getSource().getWorld() );
        return 1;
    }


    ////////////////////////////////////////////////////////////////////////////////
    public static int init( CommandContext<ServerCommandSource> context ) throws CommandSyntaxException {
        center_ = context.getSource().getPlayer().getBlockPos();
        context.getSource().sendFeedback( new LiteralText( "Set center to " + center_.toString() ), false );
        return 1;
    }


    ////////////////////////////////////////////////////////////////////////////////
    public static int run( CommandContext<ServerCommandSource> context, int mode ) throws CommandSyntaxException
    {
        final BlockState obsidian = Blocks.OBSIDIAN.getDefaultState();
        final BlockState yellowConcrete = Blocks.YELLOW_CONCRETE.getDefaultState();
        final BlockState orangeConcrete = Blocks.ORANGE_CONCRETE.getDefaultState();
        final BlockState pinkConcrete = Blocks.PINK_CONCRETE.getDefaultState();

        //  clear out area

        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();
        World world = source.getWorld();

        if ( center_ == null || mode <= 0 ) {
            center_ = player.getBlockPos();
        }

        List<BlockPos> minMax = Utility.computeBoundingBox( center_, world );
        boxMin_ = minMax.get( 0 );
        boxMax_ = minMax.get( 1 );

        Utility.clearArea( boxMin_, boxMax_, world );
        Utility.generateBackside( boxMin_, boxMax_, world );

        if ( mode == 0 ) {
            return 1;
        }

        //  project buds

        List<HashSet<BlockPos>> projs = Utility.projectBuds( boxMin_, boxMax_, world );

        projs.get( 0 ).forEach( p -> world.setBlockState( p, obsidian ));
        projs.get( 1 ).forEach( p -> world.setBlockState( p, obsidian ));
        projs.get( 2 ).forEach( p -> world.setBlockState( p, obsidian ));


        //  build contours

        LOGGER.info( "building contours..." );

        List<BlockPos> projX2D = projs.get( 0 ).stream().map( p -> Utility.pack( p, 0 ) ).toList();
        List<BlockPos> projY2D = projs.get( 1 ).stream().map( p -> Utility.pack( p, 1 ) ).toList();
        List<BlockPos> projZ2D = projs.get( 2 ).stream().map( p -> Utility.pack( p, 2 ) ).toList();
        List<List<BlockPos>> proj2Ds = List.of( projX2D, projY2D, projZ2D );

        List<BlockPos> contX = Clusters.buildContour( projX2D );
        List<BlockPos> contY = Clusters.buildContour( projY2D );
        List<BlockPos> contZ = Clusters.buildContour( projZ2D );
        List<List<BlockPos>> conts = List.of( contX, contY, contZ );

        final int off = Constants.kOffset;
        final int[] offs = { boxMax_.getX() + off, boxMax_.getY() + off, boxMax_.getZ() + off };

        for ( int dim = 0; dim < 3; dim++ ) {
            final int currDim = dim;
            conts.get( dim ).forEach( p-> world.setBlockState( Utility.unpack( p, currDim, offs[ currDim ] ), yellowConcrete ));
        }

        if ( mode >= 0 && mode <= 1 ) {
            return 1;
        }


        //  compute clusters

        LOGGER.info( "computing clusters..." );

        List<ArrayList<HashSet<BlockPos>>> clusts;

        switch ( mode ) {
            case 1: clusts = conts.stream().map( Clusters::findClusters ).toList(); break;
            case 2: clusts = IntStream.range( 0, 3 ).mapToObj( dim -> Clusters.combineClusters( conts.get( dim ), proj2Ds.get( dim ), true ) ).toList(); break;
            default: clusts = IntStream.range( 0, 3 ).mapToObj( dim -> Clusters.combineClusters( conts.get( dim ), proj2Ds.get( dim ), false ) ).toList(); break;
        }

        clusts.forEach( clust -> LOGGER.info( clust.size() ) );

        List<BlockState> fillerStates = Constants.kFillerBlocks.stream().map( Block::getDefaultState ).toList();

        for ( int dim = 0; dim < 3; dim++ ) {
            final int currDim = dim;
            for ( var i = 0; i < clusts.get( dim ).size(); i++ ) {
                BlockState state = fillerStates.get( i % fillerStates.size() );
                clusts.get( dim ).get( i ).forEach( p -> world.setBlockState( Utility.unpack( p, currDim, offs[ currDim ] ), state ) );
            }
        }

        if ( mode >= 0 && mode < 4 ) {
            return 1;
        }


        //  validate clusters

        Map<Clusters.Validity, BlockState> states = new HashMap<>();
        states.put( Clusters.Validity.Valid, Registry.BLOCK.get( new Identifier( "minecraft", "lime_concrete" ) ).getDefaultState() );
        states.put( Clusters.Validity.TooSmall, Registry.BLOCK.get( new Identifier( "minecraft", "pink_concrete" ) ).getDefaultState() );
        states.put( Clusters.Validity.TooLarge, Registry.BLOCK.get( new Identifier( "minecraft", "magenta_concrete" ) ).getDefaultState() );
        states.put( Clusters.Validity.NoStraight, Registry.BLOCK.get( new Identifier( "minecraft", "purple_concrete" ) ).getDefaultState() );

        for ( int dim = 0; dim < 3; dim++ ) {
            final int currDim = dim;
            clusts.get( dim ).forEach( cluster -> {
                final Clusters.Validity validity = Clusters.validate( cluster );
                cluster.forEach( p -> world.setBlockState( Utility.unpack( p, currDim, offs[ currDim ] ), states.get( validity ) ) );
            });
        }

        if ( mode >= 0 && mode < 5 ) {
            return 1;
        }


        //  merge too small clusters

        BlockState cryingObsidian = Registry.BLOCK.get( new Identifier( "minecraft", "crying_obsidian" ) ).getDefaultState();

        for ( int dim = 0; dim < 3; dim++ )
        {
            // LOGGER.info( "dim: " + dim );
            final int currDim = dim;

            HashSet<BlockPos> unreachables = Clusters.mergeSmallClusters( clusts.get( currDim ), proj2Ds.get( currDim ) );
            // LOGGER.info( unreachables );

            clusts.get( dim ).forEach( cluster -> {
                // LOGGER.info( "size: " + cluster.size() );
                final Clusters.Validity validity = Clusters.validate( cluster );
                cluster.forEach( p -> world.setBlockState( Utility.unpack( p, currDim, offs[ currDim ] ), states.get( validity ) ) );
            });

            unreachables.forEach( p -> world.setBlockState( Utility.unpack( p, currDim, offs[ currDim ] ), cryingObsidian ));
        }

        if ( mode < 6 ) {
            return 1;
        }


        //  split too large clusters

        ArrayList<HashSet<BlockPos>> clustC = new ArrayList<>();

        for ( int dim = 0; dim < 3; dim++ )
        {
            final int kDim = dim;

            for ( HashSet<BlockPos> clust : clusts.get( dim ) )
            {
                ArrayList<HashSet<BlockPos>> comps = new ArrayList<>();

                if ( Clusters.validate( clust ) == Clusters.Validity.Valid ) {
                    clustC.add( clust );
                    continue;
                }

                if ( Clusters.validate( clust ) != Clusters.Validity.TooLarge ) {
                    clustC.add( clust );
                    LOGGER.warn( "split: UNHANDLED CASE" );
                    continue;
                }

                comps = Clusters.split( clust );

                for ( var i = 0; i < comps.size(); i++ ) {
                    BlockState state = fillerStates.get( i % fillerStates.size() );
                    comps.get( i ).forEach( p -> world.setBlockState( Utility.unpack( p, kDim, offs[ kDim ] ), state ) );
                }
            }
        }

        return 1;
    }
}
