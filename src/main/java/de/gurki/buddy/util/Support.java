package de.gurki.buddy.util;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.Direction.AxisDirection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.HashSet;
import java.util.Iterator;


public class Support
{
    public static final String MOD_ID = "de.gurki.buddy";
    public static final Logger LOGGER = LogManager.getLogger( MOD_ID );

    public BlockPos seed = null;
    public Axis axis = null;


    ////////////////////////////////////////////////////////////////////////////////
    public boolean isEmpty() {
        return ( seed == null );
    }


    ////////////////////////////////////////////////////////////////////////////////
    public HashSet<BlockPos> getPositions()
    {
        HashSet<BlockPos> res = new HashSet<>();
        res.add( seed.toImmutable() );
        res.add( seed.offset( axis, 1 ) );
        res.add( seed.offset( axis, 2 ) );

        return res;
    }


    ////////////////////////////////////////////////////////////////////////////////
    public HashSet<Integer> getIndices( Graph graph ) {
        return new HashSet<>( getPositions().stream().map( p -> graph.ids.get( p ) ).toList() );
    }


    ////////////////////////////////////////////////////////////////////////////////
    public static Support findClosestSupport( HashSet<BlockPos> cluster, Graph graph, int startId )
    {
        Iterator<Integer> iter = graph.iterateBFS( startId ).iterator();
        Support support = null;
        Integer vid = -1;

        while ( iter.hasNext() )
        {
            vid = iter.next();
            support = findConnectedSupports( cluster, graph.verts[ vid ] );

            if ( ! support.isEmpty() ) {
                break;
            }
        }

        if ( support == null || support.isEmpty() ) {
            return new Support();
        }

        return support;
    }


    ////////////////////////////////////////////////////////////////////////////////
    public static Support findConnectedSupports( HashSet<BlockPos> cluster, BlockPos pos )
    {
        Support support = new Support();

        for ( Axis axis : Axis.values() )
        {
            final BlockPos n1 = pos.offset( axis,1 );
            final BlockPos n2 = pos.offset( axis,-1 );

            if ( cluster.contains( n1 ) && cluster.contains( n2 ) ) {
                support.seed = n2;
                support.axis = axis;
                return support;
            }
        }

        for ( Direction dir : Direction.values() )
        {
            final BlockPos n1 = pos.offset( dir );
            final BlockPos n2 = pos.offset( dir, 2 );

            if ( ! cluster.contains( n1 ) || ! cluster.contains( n2 ) ) {
                continue;
            }

            if ( dir.getDirection() == AxisDirection.POSITIVE ) {
                support.seed = pos;
            } else {
                support.seed = n2;
            }

            support.axis = dir.getAxis();
            return support;
        }

        return support;
    }
}
