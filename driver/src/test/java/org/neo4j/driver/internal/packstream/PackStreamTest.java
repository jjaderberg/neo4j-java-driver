/**
 * Copyright (c) 2002-2015 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.neo4j.driver.internal.packstream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.LinkedHashMap;
import java.util.Map;

import org.hamcrest.MatcherAssert;
import org.junit.Test;

import org.neo4j.driver.internal.util.BytePrinter;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;

import static junit.framework.TestCase.assertFalse;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class PackStreamTest
{

    public static Map<String, Object> asMap( Object... keysAndValues )
    {
        Map<String, Object> map = new LinkedHashMap<>( keysAndValues.length / 2 );
        String key = null;
        for ( Object keyOrValue : keysAndValues )
        {
            if ( key == null )
            {
                key = keyOrValue.toString();
            }
            else
            {
                map.put( key, keyOrValue );
                key = null;
            }
        }
        return map;
    }

    private static class Machine
    {

        private final ByteArrayOutputStream output;
        private final WritableByteChannel writable;
        private final PackStream.Packer packer;

        public Machine()
        {
            this.output = new ByteArrayOutputStream();
            this.writable = Channels.newChannel( this.output );
            this.packer = new PackStream.Packer( new BufferedChannelOutput( this.writable ) );
        }

        public Machine( int bufferSize )
        {
            this.output = new ByteArrayOutputStream();
            this.writable = Channels.newChannel( this.output );
            this.packer = new PackStream.Packer( new BufferedChannelOutput( this.writable, bufferSize ) );
        }

        public void reset()
        {
            output.reset();
        }

        public byte[] output()
        {
            return output.toByteArray();
        }

        public PackStream.Packer packer()
        {
            return packer;
        }
    }

    private PackStream.Unpacker newUnpacker( byte[] bytes )
    {
        ByteArrayInputStream input = new ByteArrayInputStream( bytes );
        return new PackStream.Unpacker( Channels.newChannel( input ) );
    }

    @Test
    public void testCanPackAndUnpackNull() throws Throwable
    {
        // Given
        Machine machine = new Machine();

        // When
        machine.packer().packNull();
        machine.packer().flush();

        // Then
        byte[] bytes = machine.output();
        assertThat( bytes, equalTo( new byte[]{(byte) 0xC0} ) );

        // When
        PackStream.Unpacker unpacker = newUnpacker( bytes );
        PackType packType = unpacker.peekNextType();

        // Then
        assertThat( packType, equalTo( PackType.NULL ) );

    }

    @Test
    public void testCanPackAndUnpackTrue() throws Throwable
    {
        // Given
        Machine machine = new Machine();

        // When
        machine.packer().pack( true );
        machine.packer().flush();

        // Then
        byte[] bytes = machine.output();
        assertThat( bytes, equalTo( new byte[]{(byte) 0xC3} ) );

        // When
        PackStream.Unpacker unpacker = newUnpacker( bytes );
        PackType packType = unpacker.peekNextType();

        // Then
        assertThat( packType, equalTo( PackType.BOOLEAN ) );
        assertThat( unpacker.unpackBoolean(), equalTo( true ) );

    }

    @Test
    public void testCanPackAndUnpackFalse() throws Throwable
    {
        // Given
        Machine machine = new Machine();

        // When
        machine.packer().pack( false );
        machine.packer().flush();

        // Then
        byte[] bytes = machine.output();
        assertThat( bytes, equalTo( new byte[]{(byte) 0xC2} ) );

        // When
        PackStream.Unpacker unpacker = newUnpacker( bytes );
        PackType packType = unpacker.peekNextType();

        // Then
        assertThat( packType, equalTo( PackType.BOOLEAN ) );
        assertThat( unpacker.unpackBoolean(), equalTo( false ) );

    }

    @Test
    public void testCanPackAndUnpackTinyIntegers() throws Throwable
    {
        // Given
        Machine machine = new Machine();

        for ( long i = -16; i < 128; i++ )
        {
            // When
            machine.reset();
            machine.packer().pack( i );
            machine.packer().flush();

            // Then
            byte[] bytes = machine.output();
            assertThat( bytes.length, equalTo( 1 ) );

            // When
            PackStream.Unpacker unpacker = newUnpacker( bytes );
            PackType packType = unpacker.peekNextType();

            // Then
            assertThat( packType, equalTo( PackType.INTEGER ) );
            assertThat( unpacker.unpackLong(), equalTo( i ) );
        }
    }

    @Test
    public void testCanPackAndUnpackShortIntegers() throws Throwable
    {
        // Given
        Machine machine = new Machine();

        for ( long i = -32768; i < 32768; i++ )
        {
            // When
            machine.reset();
            machine.packer().pack( i );
            machine.packer().flush();

            // Then
            byte[] bytes = machine.output();
            assertThat( bytes.length, lessThanOrEqualTo( 3 ) );

            // When
            PackStream.Unpacker unpacker = newUnpacker( bytes );
            PackType packType = unpacker.peekNextType();

            // Then
            assertThat( packType, equalTo( PackType.INTEGER ) );
            assertThat( unpacker.unpackLong(), equalTo( i ) );

        }

    }

    @Test
    public void testCanPackAndUnpackPowersOfTwoAsIntegers() throws Throwable
    {
        // Given
        Machine machine = new Machine();

        for ( int i = 0; i < 32; i++ )
        {
            long n = (long) Math.pow( 2, i );

            // When
            machine.reset();
            machine.packer().pack( n );
            machine.packer().flush();

            // Then
            PackStream.Unpacker unpacker = newUnpacker( machine.output() );
            PackType packType = unpacker.peekNextType();

            // Then
            assertThat( packType, equalTo( PackType.INTEGER ) );
            assertThat( unpacker.unpackLong(), equalTo( n ) );

        }

    }

    @Test
    public void testCanPackAndUnpackPowersOfTwoPlusABitAsDoubles() throws Throwable
    {
        // Given
        Machine machine = new Machine();

        for ( int i = 0; i < 32; i++ )
        {
            double n = Math.pow( 2, i ) + 0.5;

            // When
            machine.reset();
            machine.packer().pack( n );
            machine.packer().flush();

            // Then
            PackStream.Unpacker unpacker = newUnpacker( machine.output() );
            PackType packType = unpacker.peekNextType();

            // Then
            assertThat( packType, equalTo( PackType.FLOAT ) );
            assertThat( unpacker.unpackDouble(), equalTo( n ) );

        }

    }

    @Test
    public void testCanPackAndUnpackPowersOfTwoMinusABitAsDoubles() throws Throwable
    {
        // Given
        Machine machine = new Machine();

        for ( int i = 0; i < 32; i++ )
        {
            double n = Math.pow( 2, i ) - 0.5;

            // When
            machine.reset();
            machine.packer().pack( n );
            machine.packer().flush();

            // Then
            PackStream.Unpacker unpacker = newUnpacker( machine.output() );
            PackType packType = unpacker.peekNextType();

            // Then
            assertThat( packType, equalTo( PackType.FLOAT ) );
            assertThat( unpacker.unpackDouble(), equalTo( n ) );

        }

    }

    @Test
    public void testCanPackAndUnpackByteArrays() throws Throwable
    {
        // Given
        Machine machine = new Machine( 17000000 );

        for ( int i = 0; i < 24; i++ )
        {
            byte[] array = new byte[(int) Math.pow( 2, i )];

            // When
            machine.reset();
            machine.packer().pack( array );
            machine.packer().flush();

            // Then
            PackStream.Unpacker unpacker = newUnpacker( machine.output() );
            PackType packType = unpacker.peekNextType();

            // Then
            assertThat( packType, equalTo( PackType.BYTES ) );
            assertArrayEquals( array, unpacker.unpackBytes() );
        }
    }

    @Test
    public void testCanPackAndUnpackStrings() throws Throwable
    {
        // Given
        Machine machine = new Machine( 17000000 );

        for ( int i = 0; i < 24; i++ )
        {
            String string = new String( new byte[(int) Math.pow( 2, i )] );

            // When
            machine.reset();
            machine.packer().pack( string );
            machine.packer().flush();

            // Then
            PackStream.Unpacker unpacker = newUnpacker( machine.output() );
            PackType packType = unpacker.peekNextType();

            // Then
            assertThat( packType, equalTo( PackType.STRING ) );
            assertThat( unpacker.unpackString(), equalTo( string ) );
        }

    }

    @Test
    public void testCanPackAndUnpackBytes() throws Throwable
    {
        // Given
        Machine machine = new Machine();

        // When
        PackStream.Packer packer = machine.packer();
        packer.pack( "ABCDEFGHIJ".getBytes() );
        packer.flush();

        // Then
        PackStream.Unpacker unpacker = newUnpacker( machine.output() );
        PackType packType = unpacker.peekNextType();

        // Then
        assertThat( packType, equalTo( PackType.BYTES ) );
        assertArrayEquals( "ABCDEFGHIJ".getBytes(), unpacker.unpackBytes() );

    }

    @Test
    public void testCanPackAndUnpackString() throws Throwable
    {
        // Given
        Machine machine = new Machine();

        // When
        PackStream.Packer packer = machine.packer();
        packer.pack( "ABCDEFGHIJ" );
        packer.flush();

        // Then
        PackStream.Unpacker unpacker = newUnpacker( machine.output() );
        PackType packType = unpacker.peekNextType();

        // Then
        assertThat( packType, equalTo( PackType.STRING ) );
        assertThat( unpacker.unpackString(), equalTo( "ABCDEFGHIJ" ));

    }

    @Test
    public void testCanPackAndUnpackSpecialString() throws Throwable
    {
        // Given
        Machine machine = new Machine();
        String code = "Mjölnir";

        // When
        PackStream.Packer packer = machine.packer();
        packer.pack( code );
        packer.flush();

        // Then
        PackStream.Unpacker unpacker = newUnpacker( machine.output() );
        PackType packType = unpacker.peekNextType();

        // Then
        assertThat( packType, equalTo( PackType.STRING ) );
        assertThat( unpacker.unpackString(), equalTo( code ));
    }

    @Test
    public void testCanPackAndUnpackStringFromBytes() throws Throwable
    {
        // Given
        Machine machine = new Machine();

        // When
        PackStream.Packer packer = machine.packer();
        packer.packString( "ABCDEFGHIJ".getBytes() );
        packer.flush();

        // Then
        PackStream.Unpacker unpacker = newUnpacker( machine.output() );
        PackType packType = unpacker.peekNextType();

        // Then
        assertThat( packType, equalTo( PackType.STRING ) );
        assertThat( unpacker.unpackString(), equalTo( "ABCDEFGHIJ" ));

    }

    @Test
    public void testCanPackAndUnpackSpecialStringFromBytes() throws Throwable
    {
        // Given
        Machine machine = new Machine();
        String code = "Mjölnir"; // UTF-8 `c3 b6` for ö

        // When
        PackStream.Packer packer = machine.packer();

        byte[] bytes = code.getBytes( UTF_8 );
        MatcherAssert.assertThat( BytePrinter.hex( bytes ).trim(), equalTo( "4d 6a c3 b6 6c 6e 69 72" ) );
        assertThat( new String( bytes, UTF_8 ), equalTo( code ) );

        packer.packString( bytes );
        packer.flush();

        // Then
        PackStream.Unpacker unpacker = newUnpacker( machine.output() );
        PackType packType = unpacker.peekNextType();

        // Then
        assertThat( packType, equalTo( PackType.STRING ) );
        assertThat( unpacker.unpackString(), equalTo( code ));
    }

    @Test
    public void testCanPackAndUnpackListOneItemAtATime() throws Throwable
    {
        // Given
        Machine machine = new Machine();

        // When
        PackStream.Packer packer = machine.packer();
        packer.packListHeader( 3 );
        packer.pack( 12 );
        packer.pack( 13 );
        packer.pack( 14 );
        packer.flush();

        // Then
        PackStream.Unpacker unpacker = newUnpacker( machine.output() );
        PackType packType = unpacker.peekNextType();

        assertThat( packType, equalTo( PackType.LIST ) );
        assertThat( unpacker.unpackListHeader(), equalTo( 3l ) );
        assertThat( unpacker.unpackLong(), equalTo( 12l ) );
        assertThat( unpacker.unpackLong(), equalTo( 13l ) );
        assertThat( unpacker.unpackLong(), equalTo( 14l ) );

    }

    @Test
    public void testCanPackAndUnpackListOfString() throws Throwable
    {
        // Given
        Machine machine = new Machine();

        // When
        PackStream.Packer packer = machine.packer();
        packer.pack( asList( "eins", "zwei", "drei" ) );
        packer.flush();

        // Then
        PackStream.Unpacker unpacker = newUnpacker( machine.output() );
        PackType packType = unpacker.peekNextType();

        assertThat( packType, equalTo( PackType.LIST ) );
        assertThat( unpacker.unpackListHeader(), equalTo( 3l ) );
        assertThat( unpacker.unpackString(), equalTo( "eins" ) );
        assertThat( unpacker.unpackString(), equalTo( "zwei" ) );
        assertThat( unpacker.unpackString(), equalTo( "drei" ) );

    }

    @Test
    public void testCanPackAndUnpackListOfSpecialStrings() throws Throwable
    {
        // Given
        Machine machine = new Machine();

        // When
        PackStream.Packer packer = machine.packer();
        packer.pack( asList( "Mjölnir", "Mjölnir", "Mjölnir" ) );
        packer.flush();

        // Then
        PackStream.Unpacker unpacker = newUnpacker( machine.output() );
        PackType packType = unpacker.peekNextType();

        assertThat( packType, equalTo( PackType.LIST ) );
        assertThat( unpacker.unpackListHeader(), equalTo( 3l ) );
        assertThat( unpacker.unpackString(), equalTo( "Mjölnir" ) );
        assertThat( unpacker.unpackString(), equalTo( "Mjölnir" ) );
        assertThat( unpacker.unpackString(), equalTo( "Mjölnir" ) );
    }

    @Test
    public void testCanPackAndUnpackListOfStringOneByOne() throws Throwable
    {
        // Given
        Machine machine = new Machine();

        // When
        PackStream.Packer packer = machine.packer();
        packer.packListHeader( 3 );
        packer.flush();
        packer.pack( "eins" );
        packer.flush();
        packer.pack( "zwei" );
        packer.flush();
        packer.pack( "drei" );
        packer.flush();

        // Then
        PackStream.Unpacker unpacker = newUnpacker( machine.output() );
        PackType packType = unpacker.peekNextType();

        assertThat( packType, equalTo( PackType.LIST ) );
        assertThat( unpacker.unpackListHeader(), equalTo( 3l ) );
        assertThat( unpacker.unpackString(), equalTo( "eins" ) );
        assertThat( unpacker.unpackString(), equalTo( "zwei" ) );
        assertThat( unpacker.unpackString(), equalTo( "drei" ) );

    }

    @Test
    public void testCanPackAndUnpackListOfSpecialStringOneByOne() throws Throwable
    {
        // Given
        Machine machine = new Machine();

        // When
        PackStream.Packer packer = machine.packer();
        packer.packListHeader( 3 );
        packer.flush();
        packer.pack( "Mjölnir" );
        packer.flush();
        packer.pack( "Mjölnir" );
        packer.flush();
        packer.pack( "Mjölnir" );
        packer.flush();

        // Then
        PackStream.Unpacker unpacker = newUnpacker( machine.output() );
        PackType packType = unpacker.peekNextType();

        assertThat( packType, equalTo( PackType.LIST ) );
        assertThat( unpacker.unpackListHeader(), equalTo( 3l ) );
        assertThat( unpacker.unpackString(), equalTo( "Mjölnir" ) );
        assertThat( unpacker.unpackString(), equalTo( "Mjölnir" ) );
        assertThat( unpacker.unpackString(), equalTo( "Mjölnir" ) );

    }

    @Test
    public void testCanPackAndUnpackMap() throws Throwable
    {
        // Given
        Machine machine = new Machine();

        // When
        PackStream.Packer packer = machine.packer();
        packer.pack( asMap( "one", 1, "two", 2 ) );
        packer.flush();

        // Then
        PackStream.Unpacker unpacker = newUnpacker( machine.output() );
        PackType packType = unpacker.peekNextType();

        assertThat( packType, equalTo( PackType.MAP ) );
        assertThat( unpacker.unpackMapHeader(), equalTo( 2l ) );

        assertThat( unpacker.unpackString(), equalTo( "one") );
        assertThat( unpacker.unpackLong(), equalTo( 1l) );

        assertThat( unpacker.unpackString(), equalTo( "two") );
        assertThat( unpacker.unpackLong(), equalTo( 2l) );
    }

    @Test
    public void testCanPackAndUnpackStruct() throws Throwable
    {
        // Given
        Machine machine = new Machine();

        // When
        PackStream.Packer packer = machine.packer();
        packer.packStructHeader( 3, (byte)'N' );
        packer.pack( 12 );
        packer.pack( asList( "Person", "Employee" ) );
        packer.pack( asMap( "name", "Alice", "age", 33 ) );
        packer.flush();

        // Then
        PackStream.Unpacker unpacker = newUnpacker( machine.output() );
        PackType packType = unpacker.peekNextType();

        assertThat( packType, equalTo( PackType.STRUCT ) );
        assertThat( unpacker.unpackStructHeader(), equalTo( 3l ) );
        assertThat( unpacker.unpackStructSignature(), equalTo( (byte)'N' ) );

        assertThat( unpacker.unpackLong(), equalTo( 12l ));

        assertThat( unpacker.unpackListHeader(), equalTo( 2l ));
        assertThat( unpacker.unpackString(), equalTo( "Person" ));
        assertThat( unpacker.unpackString(), equalTo( "Employee" ));

        assertThat( unpacker.unpackMapHeader(), equalTo( 2l ));
        assertThat( unpacker.unpackString(), equalTo( "name" ));
        assertThat( unpacker.unpackString(), equalTo( "Alice" ));
        assertThat( unpacker.unpackString(), equalTo( "age" ));
        assertThat( unpacker.unpackLong(), equalTo( 33l ));
    }

    @Test
    public void testCanDoStreamingListUnpacking() throws Throwable
    {
        // Given
        Machine machine = new Machine();
        PackStream.Packer packer = machine.packer();
        packer.pack( asList(1,2,3,asList(4,5)) );
        packer.flush();

        // When I unpack this value
        PackStream.Unpacker unpacker = newUnpacker( machine.output() );

        // Then I can do streaming unpacking
        long size = unpacker.unpackListHeader();
        long a = unpacker.unpackLong();
        long b = unpacker.unpackLong();
        long c = unpacker.unpackLong();

        long innerSize = unpacker.unpackListHeader();
        long d = unpacker.unpackLong();
        long e = unpacker.unpackLong();

        // And all the values should be sane
        assertEquals( 4, size );
        assertEquals( 2, innerSize );
        assertEquals( 1, a );
        assertEquals( 2, b );
        assertEquals( 3, c );
        assertEquals( 4, d );
        assertEquals( 5, e );
    }

    @Test
    public void testCanDoStreamingStructUnpacking() throws Throwable
    {
        // Given
        Machine machine = new Machine();
        PackStream.Packer packer = machine.packer();
        packer.packStructHeader( 4, (byte)'~' );
        packer.pack( 1 );
        packer.pack( 2 );
        packer.pack( 3 );
        packer.pack( asList( 4,5 ) );
        packer.flush();

        // When I unpack this value
        PackStream.Unpacker unpacker = newUnpacker( machine.output() );

        // Then I can do streaming unpacking
        long size = unpacker.unpackStructHeader();
        byte signature = unpacker.unpackStructSignature();
        long a = unpacker.unpackLong();
        long b = unpacker.unpackLong();
        long c = unpacker.unpackLong();

        long innerSize = unpacker.unpackListHeader();
        long d = unpacker.unpackLong();
        long e = unpacker.unpackLong();

        // And all the values should be sane
        assertEquals( 4, size );
        assertEquals( '~', signature );
        assertEquals( 2, innerSize );
        assertEquals( 1, a );
        assertEquals( 2, b );
        assertEquals( 3, c );
        assertEquals( 4, d );
        assertEquals( 5, e );
    }

    @Test
    public void testCanDoStreamingMapUnpacking() throws Throwable
    {
        // Given
        Machine machine = new Machine();
        PackStream.Packer packer = machine.packer();
        packer.packMapHeader( 2 );
        packer.pack( "name" );
        packer.pack( "Bob" );
        packer.pack( "cat_ages" );
        packer.pack( asList( 4.3, true ) );
        packer.flush();

        // When I unpack this value
        PackStream.Unpacker unpacker = newUnpacker( machine.output() );

        // Then I can do streaming unpacking
        long size = unpacker.unpackMapHeader();
        String k1 = unpacker.unpackString();
        String v1 = unpacker.unpackString();
        String k2 = unpacker.unpackString();

        long innerSize = unpacker.unpackListHeader();
        double d = unpacker.unpackDouble();
        boolean e = unpacker.unpackBoolean();

        // And all the values should be sane
        assertEquals( 2, size );
        assertEquals( 2, innerSize );
        assertEquals( "name", k1 );
        assertEquals( "Bob", v1 );
        assertEquals( "cat_ages", k2 );
        assertEquals( 4.3, d, 0.0001 );
        assertEquals( true, e );
    }

    @Test
    public void testHasNext() throws Throwable
    {
        // Given
        Machine machine = new Machine();
        PackStream.Packer packer = machine.packer();
        packer.pack( "name" );
        packer.pack( 1 );
        packer.flush();

        // When I start unpacking
        PackStream.Unpacker unpacker = newUnpacker( machine.output() );

        // Then
        assertTrue( unpacker.hasNext() );

        // When I unpack the first string
        unpacker.unpackString();

        // Then
        assertTrue( unpacker.hasNext() );

        // When I unpack the integer
        unpacker.unpackLong();

        // Then
        assertFalse( unpacker.hasNext() );
    }

    @Test
    public void handlesDataCrossingBufferBoundaries() throws Throwable
    {
        // Given
        Machine machine = new Machine();
        PackStream.Packer packer = machine.packer();
        packer.pack( Long.MAX_VALUE );
        packer.pack( Long.MAX_VALUE );
        packer.flush();

        ReadableByteChannel ch = Channels.newChannel( new ByteArrayInputStream( machine.output() ) );
        PackStream.Unpacker unpacker = new PackStream.Unpacker( 11 );
        unpacker.reset( ch );

        // Serialized ch will look like, and misalign with the 11-byte unpack buffer:

        // [XX][XX][XX][XX][XX][XX][XX][XX][XX][XX][XX][XX][XX][XX][XX][XX][XX][XX]
        //  mkr \___________data______________/ mkr \___________data______________/
        // \____________unpack buffer_________________/

        // When
        long first = unpacker.unpackLong();
        long second = unpacker.unpackLong();

        // Then
        assertEquals(Long.MAX_VALUE, first);
        assertEquals(Long.MAX_VALUE, second);
    }

    @Test
    public void testCanPeekOnNextType() throws Throwable
    {
        // When & Then
        assertPeekType( PackType.STRING, "a string" );
        assertPeekType( PackType.INTEGER, 123 );
        assertPeekType( PackType.FLOAT, 123.123 );
        assertPeekType( PackType.BOOLEAN, true );
        assertPeekType( PackType.LIST, asList( 1,2,3 ) );
        assertPeekType( PackType.MAP, asMap( "l",3 ) );
    }

    void assertPeekType( PackType type, Object value ) throws IOException
    {
        // Given
        Machine machine = new Machine();
        PackStream.Packer packer = machine.packer();
        packer.pack( value );
        packer.flush();

        PackStream.Unpacker unpacker = newUnpacker( machine.output() );

        // When & Then
        assertEquals( type, unpacker.peekNextType() );
    }
}
