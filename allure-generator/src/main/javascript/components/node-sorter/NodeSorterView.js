import './styles.css';
import {on, className} from '../../decorators';
import settings from '../../util/settings';
import template from './NodeSorterView.hbs';
import {View} from 'backbone.marionette';
import {values} from '../../util/statuses';


@className('sorter')
class NodeSorterView extends View {
    template = template;

    sorters = [
        {
            key: 'name',
            sorter: (a, b, c) => {return a.name.toLowerCase() < b.name.toLowerCase() ? -c : c;}
        },
        {
            key: 'duration',
            sorter: (a, b, c) => {return a.time.duration < b.time.duration ? -c : c;}
        },
        {
            key: 'status',
            sorter: (a, b, c) => {
                if ('status' in a && 'status' in b){
                    return values.indexOf(a.status) > values.indexOf(b.status) ? -c : c;
                } else if ('statistic' in a && 'statistic' in b){
                    return values.reduce((all, current) => {
                        if ((a.statistic[current] !== b.statistic[current]) && all === 0) {
                            return b.statistic[current] > a.statistic[current];
                        } else {
                            return all;
                        }
                    }, 0) ? -c: c;
                } else {
                    return 1;
                }
            }
        },
    ];

    initialize({sorterSettingsKey}) {
        this.sorterSettingsKey = sorterSettingsKey;
    }

    getSorter(){
        const sortSettings = settings.getTreeSorting(this.sorterSettingsKey);
        const sorter = this.sorters[sortSettings.sorter].sorter;
        const direction =  sortSettings.ascending ? 1 : -1;
        return (a, b) => sorter(a, b, direction);
    }

    @on('click .sorter__item')
    onChangeSorting(e){
        const el = this.$(e.currentTarget);
        settings.save(this.sorterSettingsKey, {
            sorter: el.data('index'),
            ascending: !el.data('asc')
        });

        this.$('.sorter_enabled').toggleClass('sorter_enabled');
        el.data('asc', !el.data('asc'));
        el.find('.sorter__name').toggleClass('sorter_enabled');
        el.find(el.data('asc')? '.fa-sort-asc' : '.fa-sort-desc').toggleClass('sorter_enabled');
    }

    serializeData() {
        const sortSettings = settings.getTreeSorting(this.sorterSettingsKey);
        return {
            sorter: this.sorters.map((sorter, index) => ({
                index: index,
                name : sorter.key,
                asc: (sortSettings.sorter === index) && sortSettings.ascending,
                desc: (sortSettings.sorter === index) && !sortSettings.ascending
            }))
        };
    }
}

export default NodeSorterView;