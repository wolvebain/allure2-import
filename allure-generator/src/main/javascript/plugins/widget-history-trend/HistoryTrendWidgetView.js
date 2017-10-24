import './styles.scss';
import template from './HistoryTrendWidgetView.hbs';
import {View} from 'backbone.marionette';
import {className, regions} from '../../decorators/index';
import TrendChartView from '../../components/graph-trend-chart/TrendChartView';

@regions({
    chart: '.history-trend__chart'
})
@className('history-trend')
class HistoryTrendWidgetView extends View {

    template = template;

    onRender() {
        this.showChildView('chart', new TrendChartView({
            items: this.model.getWidgetData('history-trend').get('items')
        }));
    }
}

export default HistoryTrendWidgetView;
