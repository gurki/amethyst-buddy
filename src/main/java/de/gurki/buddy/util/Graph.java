package de.gurki.buddy.util;
import net.minecraft.util.math.BlockPos;
import java.util.HashSet;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.LinkedList;
import java.util.function.Predicate;

import com.google.common.collect.AbstractIterator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class Graph
{
    public static final String MOD_ID = "de.gurki.buddy";
	public static final Logger LOGGER = LogManager.getLogger( MOD_ID );

    public final BlockPos[] verts;
    public final HashMap<BlockPos, Integer> ids;
    public final HashMap<Integer, ArrayList<Integer>> edges;
    public int[][] dists;
    public int[] closeness;


    ////////////////////////////////////////////////////////////////////////////////
    public Graph( HashSet<BlockPos> cluster )
    {
        verts = new BlockPos[ cluster.size() ];
        edges = new HashMap<>();
        ids = new HashMap<>();

        int i = 0;

        for ( BlockPos p : cluster ) {
            edges.put( i, new ArrayList<>() );
            verts[ i ] = p;
            ids.put( p, i );
            i++;
        }

        computePairwiseDistance();
        computeCloseness();
    }


    ////////////////////////////////////////////////////////////////////////////////
    //  get two vertices that are as fair apart as possible
    public Integer[] getStablePair()
    {
        Integer[] sp = new Integer[ 2 ];

        int maxDist = 0;
        sp[ 0 ] = -1;
        sp[ 1 ] = -1;

        for ( int i = 0; i < verts.length; i++ ) {
            for ( int j = i + 1; j < verts.length; j++ )
            {
                final int dist = dists[ i ][ j ];

                if ( dist <= maxDist ) {
                    continue;
                }

                maxDist = dist;
                sp[ 0 ] = i;
                sp[ 1 ] = j;
            }
        }

        return sp;
    }


    ////////////////////////////////////////////////////////////////////////////////
    //  get two vertices that are as fair apart as possible from a specific set
    public Integer[] getStablePair( HashSet<Integer> cluster )
    {
        Integer[] sp = new Integer[ 2 ];

        int maxDist = 0;
        sp[ 0 ] = -1;
        sp[ 1 ] = -1;

        for ( Integer i : cluster ) {
            for ( Integer j : cluster )
            {
                if ( i >= j ) continue;
                final int dist = dists[ i ][ j ];

                if ( dist <= maxDist ) {
                    continue;
                }

                maxDist = dist;
                sp[ 0 ] = i;
                sp[ 1 ] = j;
            }
        }

        return sp;
    }


    ////////////////////////////////////////////////////////////////////////////////
    public Iterable<Integer> iterateBFS( Integer startId )
    {
        if ( startId < 0 || startId >= verts.length ) {
            return null;
        }

        return () -> new AbstractIterator<Integer>()
        {
            private Queue<Integer> next = new LinkedList<>();
            private HashSet<Integer> done = new HashSet<>();
            {
                this.next.add( startId );
            }

            @Override
            protected Integer computeNext()
            {
                if ( this.next.isEmpty() ) {
                    return (Integer)this.endOfData();
                }

                Integer currId = this.next.poll();
                this.done.add( currId );
                this.next.addAll( edges.get( currId ).stream().filter( i -> ! this.done.contains( i ) ).toList() );
                return currId;
            }
        };
    }


    ////////////////////////////////////////////////////////////////////////////////
    public Iterable<Integer> iterateBFS( Integer startId, final Predicate<Integer> pred )
    {
        if ( startId < 0 || startId >= verts.length ) {
            return null;
        }

        return () -> new AbstractIterator<Integer>()
        {
            private final Queue<Integer> next = new LinkedList<>();
            private final HashSet<Integer> done = new HashSet<>();
            {
                this.next.add( startId );
            }

            @Override
            protected Integer computeNext()
            {
                if ( this.next.isEmpty() ) {
                    return (Integer)this.endOfData();
                }

                Integer currId = this.next.poll();
                this.done.add( currId );
                this.next.addAll( edges.get( currId ).stream().filter( i -> pred.test( i ) && ! this.done.contains( i ) ).toList() );
                return currId;
            }
        };
    }


    ////////////////////////////////////////////////////////////////////////////////
    private void computePairwiseDistance()
    {
        if ( dists == null || dists.length != verts.length ) {
            dists = new int[ verts.length ][ verts.length ];
        }

        for ( int i = 0; i < verts.length; i++ )
        {
            final BlockPos v1 = verts[ i ];
            final HashSet<BlockPos> neighs = new HashSet<BlockPos>( List.of( v1.up(), v1.east(), v1.down(), v1.west(), v1.north(), v1.south() ) );
            dists[ i ][ i ] = 0;

            for ( int j = i + 1; j < verts.length; j++ )
            {
                final BlockPos v2 = verts[ j ];
                final boolean isNeighbour = neighs.contains( v2 );
                final int dist = ( isNeighbour ? 1 : Integer.MAX_VALUE );

                dists[ i ][ j ] = dist;
                dists[ j ][ i ] = dist;

                if ( isNeighbour ) {
                    edges.get( i ).add( j );
                    edges.get( j ).add( i );
                }
            }
        }

        for ( int k = 0; k < verts.length; k++ ) {
        for ( int i = 0; i < verts.length; i++ ) {
        for ( int j = 0; j < verts.length; j++ ) {
            if ( dists[ i ][ k ] == Integer.MAX_VALUE || dists[ k ][ j ] == Integer.MAX_VALUE ) continue;
            final int reroute = dists[ i ][ k ] + dists[ k ][ j ];
            if ( dists[ i ][ j ] > reroute ) dists[ i ][ j ] = reroute;
        }}}
    }


    ////////////////////////////////////////////////////////////////////////////////
    private void computeCloseness()
    {
        if ( closeness == null || closeness.length != verts.length ) {
            closeness = new int[ verts.length ];
        }

        for ( int i = 0; i < verts.length; i++ )
        {
            closeness[ i ] = 0;

            for ( int j = 0; j < verts.length; j++ ) {
                closeness[ i ] += dists[ i ][ j ];
            }
        }
    }
}
