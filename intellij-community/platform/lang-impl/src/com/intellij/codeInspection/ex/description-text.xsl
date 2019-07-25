<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
  <xsl:output method="text"/>

  <!-- Inspection descriptions: -->
  <xsl:template match="/html/body/font|/font">
    <xsl:value-of select="."/>
  </xsl:template>

</xsl:stylesheet>