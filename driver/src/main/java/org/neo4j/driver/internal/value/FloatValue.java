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

public class FloatValue extends ValueAdapter
{
    private final double val;

    public FloatValue( double val )
    {
        this.val = val;
    }

    @Override
    public String javaString()
    {
        return Double.toString( val );
    }

    @Override
    public int javaInteger()
    {
        return (int) val;
    }

    @Override
    public long javaLong()
    {
        return (long) val;
    }

    @Override
    public float javaFloat()
    {
        return (float) val;
    }

    @Override
    public boolean javaBoolean()
    {
        return val != 0;
    }

    @Override
    public double javaDouble()
    {
        return val;
    }

    @Override
    public boolean isFloat()
    {
        return true;
    }

    @Override
    public String toString()
    {
        return "float<" + val + ">";
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( o == null || getClass() != o.getClass() )
        {
            return false;
        }

        FloatValue values = (FloatValue) o;

        return Double.compare( values.val, val ) == 0;

    }

    @Override
    public int hashCode()
    {
        long temp = Double.doubleToLongBits( val );
        return (int) (temp ^ (temp >>> 32));
    }
}