/*
 * Customer Credit Rating
 * This service domain maintains and administers the bank's credit assessment for customers.
 *
 * OpenAPI spec version: 2.0.0
 * 
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */

package com.redhat.mercury.customercreditrating.model;

import java.util.Objects;
import java.util.Arrays;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.IOException;
/**
 * BQAlertsRetrieveOutputModelAlertsInstanceAnalysis
 */

@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.JavaClientCodegen", date = "2021-06-03T15:34:54.132884+02:00[Europe/Madrid]")
public class BQAlertsRetrieveOutputModelAlertsInstanceAnalysis {
  @SerializedName("alertsInstanceAnalysisRecord")
  private Object alertsInstanceAnalysisRecord = null;

  @SerializedName("alertsInstanceAnalysisReportType")
  private String alertsInstanceAnalysisReportType = null;

  @SerializedName("alertsInstanceAnalysisParameters")
  private String alertsInstanceAnalysisParameters = null;

  @SerializedName("alertsInstanceAnalysisReport")
  private Object alertsInstanceAnalysisReport = null;

  public BQAlertsRetrieveOutputModelAlertsInstanceAnalysis alertsInstanceAnalysisRecord(Object alertsInstanceAnalysisRecord) {
    this.alertsInstanceAnalysisRecord = alertsInstanceAnalysisRecord;
    return this;
  }

   /**
   * &#x60;status: Not Mapped&#x60;  core-data-type-reference: BIAN::DataTypesLibrary::CoreDataTypes::UNCEFACT::Binary  general-info: The inputs and results of the instance analysis that can be on-going, periodic and actual and projected 
   * @return alertsInstanceAnalysisRecord
  **/
  @Schema(description = "`status: Not Mapped`  core-data-type-reference: BIAN::DataTypesLibrary::CoreDataTypes::UNCEFACT::Binary  general-info: The inputs and results of the instance analysis that can be on-going, periodic and actual and projected ")
  public Object getAlertsInstanceAnalysisRecord() {
    return alertsInstanceAnalysisRecord;
  }

  public void setAlertsInstanceAnalysisRecord(Object alertsInstanceAnalysisRecord) {
    this.alertsInstanceAnalysisRecord = alertsInstanceAnalysisRecord;
  }

  public BQAlertsRetrieveOutputModelAlertsInstanceAnalysis alertsInstanceAnalysisReportType(String alertsInstanceAnalysisReportType) {
    this.alertsInstanceAnalysisReportType = alertsInstanceAnalysisReportType;
    return this;
  }

   /**
   * &#x60;status: Not Mapped&#x60;  core-data-type-reference: BIAN::DataTypesLibrary::CoreDataTypes::UNCEFACT::Code  general-info: The type of external performance analysis report available 
   * @return alertsInstanceAnalysisReportType
  **/
  @Schema(description = "`status: Not Mapped`  core-data-type-reference: BIAN::DataTypesLibrary::CoreDataTypes::UNCEFACT::Code  general-info: The type of external performance analysis report available ")
  public String getAlertsInstanceAnalysisReportType() {
    return alertsInstanceAnalysisReportType;
  }

  public void setAlertsInstanceAnalysisReportType(String alertsInstanceAnalysisReportType) {
    this.alertsInstanceAnalysisReportType = alertsInstanceAnalysisReportType;
  }

  public BQAlertsRetrieveOutputModelAlertsInstanceAnalysis alertsInstanceAnalysisParameters(String alertsInstanceAnalysisParameters) {
    this.alertsInstanceAnalysisParameters = alertsInstanceAnalysisParameters;
    return this;
  }

   /**
   * &#x60;status: Not Mapped&#x60;  core-data-type-reference: BIAN::DataTypesLibrary::CoreDataTypes::UNCEFACT::Text  general-info: The selection parameters for the analysis (e.g. period, algorithm type) 
   * @return alertsInstanceAnalysisParameters
  **/
  @Schema(description = "`status: Not Mapped`  core-data-type-reference: BIAN::DataTypesLibrary::CoreDataTypes::UNCEFACT::Text  general-info: The selection parameters for the analysis (e.g. period, algorithm type) ")
  public String getAlertsInstanceAnalysisParameters() {
    return alertsInstanceAnalysisParameters;
  }

  public void setAlertsInstanceAnalysisParameters(String alertsInstanceAnalysisParameters) {
    this.alertsInstanceAnalysisParameters = alertsInstanceAnalysisParameters;
  }

  public BQAlertsRetrieveOutputModelAlertsInstanceAnalysis alertsInstanceAnalysisReport(Object alertsInstanceAnalysisReport) {
    this.alertsInstanceAnalysisReport = alertsInstanceAnalysisReport;
    return this;
  }

   /**
   * &#x60;status: Not Mapped&#x60;  core-data-type-reference: BIAN::DataTypesLibrary::CoreDataTypes::UNCEFACT::Binary  general-info: The external analysis report in any suitable form including selection filters where appropriate 
   * @return alertsInstanceAnalysisReport
  **/
  @Schema(description = "`status: Not Mapped`  core-data-type-reference: BIAN::DataTypesLibrary::CoreDataTypes::UNCEFACT::Binary  general-info: The external analysis report in any suitable form including selection filters where appropriate ")
  public Object getAlertsInstanceAnalysisReport() {
    return alertsInstanceAnalysisReport;
  }

  public void setAlertsInstanceAnalysisReport(Object alertsInstanceAnalysisReport) {
    this.alertsInstanceAnalysisReport = alertsInstanceAnalysisReport;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BQAlertsRetrieveOutputModelAlertsInstanceAnalysis bqAlertsRetrieveOutputModelAlertsInstanceAnalysis = (BQAlertsRetrieveOutputModelAlertsInstanceAnalysis) o;
    return Objects.equals(this.alertsInstanceAnalysisRecord, bqAlertsRetrieveOutputModelAlertsInstanceAnalysis.alertsInstanceAnalysisRecord) &&
        Objects.equals(this.alertsInstanceAnalysisReportType, bqAlertsRetrieveOutputModelAlertsInstanceAnalysis.alertsInstanceAnalysisReportType) &&
        Objects.equals(this.alertsInstanceAnalysisParameters, bqAlertsRetrieveOutputModelAlertsInstanceAnalysis.alertsInstanceAnalysisParameters) &&
        Objects.equals(this.alertsInstanceAnalysisReport, bqAlertsRetrieveOutputModelAlertsInstanceAnalysis.alertsInstanceAnalysisReport);
  }

  @Override
  public int hashCode() {
    return Objects.hash(alertsInstanceAnalysisRecord, alertsInstanceAnalysisReportType, alertsInstanceAnalysisParameters, alertsInstanceAnalysisReport);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class BQAlertsRetrieveOutputModelAlertsInstanceAnalysis {\n");
    
    sb.append("    alertsInstanceAnalysisRecord: ").append(toIndentedString(alertsInstanceAnalysisRecord)).append("\n");
    sb.append("    alertsInstanceAnalysisReportType: ").append(toIndentedString(alertsInstanceAnalysisReportType)).append("\n");
    sb.append("    alertsInstanceAnalysisParameters: ").append(toIndentedString(alertsInstanceAnalysisParameters)).append("\n");
    sb.append("    alertsInstanceAnalysisReport: ").append(toIndentedString(alertsInstanceAnalysisReport)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(java.lang.Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }

}
