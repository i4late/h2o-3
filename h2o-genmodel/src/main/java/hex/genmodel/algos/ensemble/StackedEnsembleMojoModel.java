package hex.genmodel.algos.ensemble;

import hex.genmodel.MojoModel;

public class StackedEnsembleMojoModel extends MojoModel {

    MojoModel _metaLearner; //Currently only a GLM. May change to be DRF, GBM, XGBoost, or DL in the future
    MojoModel[] _baseModels; //An array of base models
    int _baseModelNum;

    public StackedEnsembleMojoModel(String[] columns, String[][] domains, String responseColumn) {
        super(columns, domains, responseColumn);
    }

    @Override
    public double[] score0(double[] row, double[] preds) {
        double[] basePreds = new double[_baseModelNum]; //Proper allocation for binomial and regression ensemble (one prediction per base model)
        if(_nclasses > 2) {
            basePreds = new double[]{_baseModelNum * _nclasses}; //Proper allocation for multinomial ensemble (class probabilities per base model)
        }
        double[] basePredsRow = new double[preds.length];
        for(int i = 0; i < _baseModelNum; ++i){
            if(_nclasses == 1) { //Regression
                _baseModels[i].score0(row, basePredsRow);
                basePreds[i] = basePredsRow[0];
            }else if(_nclasses == 2){ //Binomial
                _baseModels[i].score0(row, basePredsRow);
                basePreds[i] = basePredsRow[2];
            }else{ //Multinomial
                for(int j = 0; j < _nclasses; ++j){
                    basePreds[j] = _baseModels[i].score0(row, basePredsRow)[j+1]; //First index is the class prediction, which we are not interested in for the base models
                }
            }
        }
        _metaLearner.score0(basePreds, preds);
        return preds;
    }


}