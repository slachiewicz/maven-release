/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

try
{
    File buildLog = new File( basedir, "build.log" );

    System.out.println( "Checking logs.." );

    StringBuffer data = new StringBuffer( 1024 );
    BufferedReader reader = new BufferedReader( new FileReader( buildLog ) );
    char[] buf = new char[1024];
    int numRead = 0;
    while ( ( numRead = reader.read( buf ) ) != -1 )
    {
        String readData = String.valueOf( buf, 0, numRead );
        data.append( readData );
        buf = new char[1024];
    }
    reader.close();
    String contents = data.toString();

    String expected = "Executing preparation goals";
    int index = 0;

    if( ( index = contents.indexOf( expected, index ) ) == -1 )
    {
        System.out.println( "FAILED!" );
        return false;
    }
    expected = "Executing goals 'validate'";

    if( ( index = contents.indexOf( expected, index ) ) == -1 )
    {
        System.out.println( "FAILED!" );
        return false;
    }
    expected = "Executing completion goals";

    if( ( index = contents.indexOf( expected, index ) ) == -1 )
    {
        System.out.println( "FAILED!" );
        return false;
    }
    expected = "Executing goals 'verify'";

    if( ( index = contents.indexOf( expected, index ) ) == -1 )
    {
        System.out.println( "FAILED!" );
        return false;
    }
}
catch( Throwable t )
{
    t.printStackTrace();
    return false;
}

return true;
