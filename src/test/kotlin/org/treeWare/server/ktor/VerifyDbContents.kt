package org.treeWare.server.ktor

import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.cql.ResultSet
import org.treeWare.model.getFileReader
import java.io.StringWriter
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

fun verifyQueryResults(expectedResultsFile: String, cqlSession: CqlSession, query: String) {
    val resultWriter = StringWriter()
    val result: ResultSet = cqlSession.execute(query)
    result.all().forEach {
        resultWriter.write(it.formattedContents)
        resultWriter.write("\n")
    }
    val actual = resultWriter.toString()

    verifyDbContents(expectedResultsFile, actual)
}

fun verifyKeyspaceContents(expectedResultsFile: String, cqlSession: CqlSession, keyspace: String) {
    // Get the names of all tables in the specified keyspace.
    val getTables = "SELECT table_name FROM system_schema.tables WHERE keyspace_name='$keyspace'"
    val tables: ResultSet = cqlSession.execute(getTables)

    val resultWriter = StringWriter()
    tables.all().forEach { tableRow ->
        val table = tableRow.getString("table_name")
        resultWriter.write("table: $keyspace.$table\n")

        // Get content of each table.
        val getContent = "SELECT * FROM $keyspace.$table"
        val content: ResultSet = cqlSession.execute(getContent)

        content.all().forEach {
            resultWriter.write(it.formattedContents)
            resultWriter.write("\n")
        }
    }
    val actual = resultWriter.toString()

    verifyDbContents(expectedResultsFile, actual)
}

private fun verifyDbContents(expectedResultsFile: String, actual: String) {
    val expectedReader = getFileReader(expectedResultsFile)
    assertNotNull(expectedReader)
    val expected = expectedReader.readText()

    assertEquals(expected, actual)
}
