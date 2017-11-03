setwd(normalizePath(dirname(R.utils::commandArgs(asValues=TRUE)$"f")))
source("../../scripts/h2o-r-test-setup.R")
##
# PUBDEV_4727: Groupby median fix
##

test <- function(conn) {
  Log.info("Generateing random dataset ...")
  numrows = 100
  dfEnum.hex <- h2o.createFrame(rows=numrows, col=2, categorical_fraction=1, integer_fraction=0, binary_fraction=0,
  time_fraction=0, string_fraction=0, missing_fraction=0.0, has_response=FALSE, factor=5)
  dfReal.hex <- h2o.createFrame(rows=numrows, col=3, categorical_fraction=0, integer_fraction=0, binary_fraction=0,
  time_fraction=0, string_fraction=0, missing_fraction=0.0, has_response=FALSE)  # real numbers
  df.hex <- h2o.cbind(dfEnum.hex, dfReal.hex)
  
  browser()
  column_names <- names(df.hex)

  aggregated_median <- h2o.group_by(data = df.hex, by = column_names[1:2], mean(column_names[3:length(column_names)]), gb.control=list(na.methods="rm"))
  r_medium <- sapply(as.data.frame(aggregated_median)[,3], median)
  checkEqualsNumeric(r_medium, aggregated_median)  # compare R and h2o.groupby answers
}

doTest("Testing groupby median", test)

