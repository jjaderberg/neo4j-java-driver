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
package org.neo4j.driver.internal.value;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class BooleanValueTest
{

    @Test
    public void testBooleanTrue() throws Exception
    {
        // Given
        BooleanValue value = new BooleanValue( true );

        // Then
        assertThat( value.javaBoolean(), equalTo( true ) );
        assertThat( value.javaInteger(), equalTo( 1 ) );
        assertThat( value.javaLong(), equalTo( 1L ) );
        assertThat( value.javaFloat(), equalTo( (float) 1.0 ) );
        assertThat( value.javaDouble(), equalTo( 1.0 ) );
    }

    @Test
    public void testBooleanFalse() throws Exception
    {
        // Given
        BooleanValue value = new BooleanValue( false );

        // Then
        assertThat( value.javaBoolean(), equalTo( false ) );
        assertThat( value.javaInteger(), equalTo( 0 ) );
        assertThat( value.javaLong(), equalTo( 0L ) );
        assertThat( value.javaFloat(), equalTo( (float) 0.0 ) );
        assertThat( value.javaDouble(), equalTo( 0.0 ) );
    }

    @Test
    public void testIsBoolean() throws Exception
    {
        // Given
        BooleanValue value = new BooleanValue( true );

        // Then
        assertThat( value.isBoolean(), equalTo( true ) );
    }

    @Test
    public void testEquals() throws Exception
    {
        // Given
        BooleanValue firstValue = new BooleanValue( true );
        BooleanValue secondValue = new BooleanValue( true );

        // Then
        assertThat( firstValue, equalTo( secondValue ) );
    }

    @Test
    public void testHashCode() throws Exception
    {
        // Given
        BooleanValue value = new BooleanValue( true );

        // Then
        assertThat( value.hashCode(), notNullValue() );
    }
}