import './styles.css';
import BaseChartView from '../../components/chart/BaseChartView';
import {once} from 'underscore';
import {className} from '../../decorators';
import duration from '../../helpers/duration';

import {scaleLinear, scaleBand} from 'd3-scale';
import {select, event as currentEvent} from 'd3-selection';
import {brushX} from 'd3-brush';
import 'd3-selection-multi';
import escape from '../../util/escape';
import TooltipView from '../../components/tooltip/TooltipView';


const HOST_TITLE_HEIGHT = 20;
const BRUSH_HEIGHT = 30;
const BAR_HEIGHT = 15;
const BAR_GAP = 2;
const PADDING = 30;


@className('timeline')
class TimelineView extends BaseChartView {

    initialize() {
        this.chartX = scaleLinear();
        this.brushX = scaleLinear();
        this.brush = brushX().on('brush', this.onBrushChange.bind(this));

        this.tooltip = new TooltipView({position: 'bottom'});

        this.minDuration = 0;
        this.selectedDuration = 0;
        this.maxDuration = this.model.get('time').maxDuration;
        this.data = this.model.getFilteredData(this.minDuration);
    }

    onAttach(waitTransition) {
        if(waitTransition || this.firstRender) {
            const callback = once(() => this.doShow());
            this.$el.parent().one('transitionend', callback);
            setTimeout(callback, 500);
        } else {
            this.doShow();
        }
    }

    setupViewport() {
        var selectedPercent = (100*this.data.selectedTestCases/this.model.get('statistic').total).toFixed(2);
        this.$el.html(`<div class="timeline__header">
            <span class="timeline__header__min_duration">${duration(this.minDuration)}</span>
            <span class="timeline__current_duration">
                Selected ${this.data.selectedTestCases}(${selectedPercent}%) tests
                with duration above ${duration(this.selectedDuration|0, 2)}
            </span>
            <span class="timeline__header__max_duration">${duration(this.maxDuration, 2)}</span>
            <input class="timeline__range_bar"
                type="range"
                min=${this.minDuration}
                max=${this.maxDuration}
                value=${this.selectedDuration}>
            </input>
        </div>
        <div class="timeline__body">
            <div class='timeline__chart'>
                <svg class="timeline__chart_svg">
                    <g class="timeline__chart__axis timeline__chart__axis_x"></g>
                    <g class="timeline__plot"></g>
                </svg>
            </div>
            <div class='timeline__brush'>
                <svg class="timeline__brush_svg">
                    <rect class="timeline__brush__axis_bg"
                        width="${this.$el.width()}"
                        height="${PADDING}"
                        fill="white"
                        transform="translate(0, ${BRUSH_HEIGHT + BAR_GAP})">
                    </rect>
                    <g class="timeline__brush__axis timeline__brush__axis_x"></g>
                </svg>
            </div>
        </div>`);
        this.svgChart = select(this.$el[0]).select('.timeline__chart_svg');
        this.svgBrush = select(this.$el[0]).select('.timeline__brush_svg');

        select(this.$el[0]).select('.timeline__range_bar')
            .on('change', this.onChangeFilter.bind(this))
            .on('input', this.onInputFilter.bind(this));
    }

    onInputFilter() {
        var value = currentEvent.target.value;
        select(this.$el[0]).select('.timeline__current_duration').text(duration(value|0, 2));
    }

    onChangeFilter() {
       this.selectedDuration = currentEvent.target.value;
       this.data = this.model.getFilteredData(this.selectedDuration);
       this.doShow();
    }

    doShow() {
        const width = this.$el. width() > 2 * PADDING ? this.$el.width() - 2 * PADDING : this.$el.width();

        this.minX = this.model.get('time').start;
        this.maxX = this.model.get('time').duration;

        this.chartX.domain([0, this.maxX]).nice();
        this.chartX.range([0, width]);

        this.brushX.domain([0, this.maxX]).nice();
        this.brushX.range([0, width]);

        this.setupViewport();

        var height = 0;
        this.data['children'].forEach(host => {
            height += this.drawHost(host, height);
        });

        select(this.$el[0]).select('.timeline__brush')
            .style('top', () => { return Math.min(this.$el.height() - BRUSH_HEIGHT, height + 2* PADDING) + 'px'; });

        this.xChartAxis = this.makeBottomAxis(this.svgChart.select('.timeline__chart__axis_x'), {
            scale: this.chartX,
            ticks: 8,
            tickFormat: d => duration(d, 2),
            tickSizeOuter: 0,
            tickSizeInner: height + BRUSH_HEIGHT + PADDING + BAR_GAP*2
        }, {
            top:  0,
            left: PADDING
        });

        this.xBrushAxis = this.makeBottomAxis(this.svgBrush.select('.timeline__brush__axis_x'), {
            scale: this.chartX,
            ticks: 8,
            tickFormat: d => duration(d, 2),
            tickSizeOuter: 0
        }, {
            top:  BRUSH_HEIGHT + BAR_GAP,
            left: PADDING
        });

        this.brush.extent([[0, 0], [width, BRUSH_HEIGHT]]);
        this.svgBrush.append('g')
            .attrs({ transform: `translate(${PADDING}, 0)` })
            .attr('class', 'brush')
            .call(this.brush)
            .call(this.brush.move, this.chartX.range());

        var handle = this.svgBrush
            .append('g')
            .attrs({transform: `translate(${PADDING}, ${BRUSH_HEIGHT/2})`});

        handle.append('text')
            .attr('class', 'timeline__left_handle')
            .text(() => { return '\uf0d9'; });

        handle.append('text')
            .attr('class', 'timeline__right_handle')
            .attr('x', width)
            .text(() => { return '\uf0da'; });

        if(this.firstRender) {
            this.svgBrush.select('.brush')
                .transition().duration(300)
                .call(this.brush.move, [1/16 * this.chartX(this.maxX), 15/16 * this.chartX(this.maxX)])
                .transition().duration(500)
                .call(this.brush.move, this.chartX.range());
        }

        this.svgChart.style('height', height + BRUSH_HEIGHT + PADDING);
        super.onRender();
    }

    drawHost(host, offset) {
        const height = host.children.length * (BAR_GAP + BAR_HEIGHT) + HOST_TITLE_HEIGHT;
        const testCases = this.model.getTestcases(host);

        const y = scaleBand()
            .domain(host.children.map(d => d.name))
            .range([HOST_TITLE_HEIGHT, height]);

        const group = this.svgChart.select('.timeline__plot').append('g').attrs({
            'class': 'timeline__group',
            transform: `translate(${PADDING}, ${offset})`
        });

        group.append('rect').attrs({
             'class': 'timeline__host-bg',
             x: -PADDING/4*3,
             y: 4,
             width: host.name.length * 6,
             height: HOST_TITLE_HEIGHT-7
        });

        group.append('text').text(host.name).attrs({
            'class': 'timeline__host',
            x: -PADDING/4*3+4,
            y: HOST_TITLE_HEIGHT/2
        });

        var bars = group.selectAll('.timeline__item ')
            .data(testCases).enter()
            .append('a')
            .attrs({
                'xlink:href': d => '#testcase/' + d.uid
            })
            .append('rect')
            .attrs({
                'class': d => 'timeline__item chart__fill_status_' + d.status,
                x: d => this.chartX(d.start),
                y: d => y(d.thread) + BAR_GAP,
                rx: 2,
                ry: 2,
                width: d => this.chartX(d.stop) - this.chartX(d.start),
                height: BAR_HEIGHT
            });

        this.bindTooltip(bars);
        bars.on('click', this.hideTooltip.bind(this));

        return height;
    }

    onBrushChange() {
        var selection = currentEvent.selection;
        var maxRight = this.chartX.range()[1];

        if (selection) {
            this.chartX.domain(selection.map(this.brushX.invert, this.brushX));
            this.svgChart.selectAll('.timeline__item').attrs({
                x: d => Math.max(0, Math.min(this.chartX(d.start), maxRight)),
                width: d => Math.max(0, Math.min(this.chartX(d.stop), maxRight)) - Math.max(0, Math.min(this.chartX(d.start), maxRight))
            });
            this.svgBrush.selectAll('.timeline__left_handle').attr('x', selection[0]);
            this.svgBrush.selectAll('.timeline__right_handle').attr('x', selection[1]);
            this.svgBrush.select('.timeline__brush__axis_x').call(this.xBrushAxis);
            this.svgChart.select('.timeline__chart__axis_x').call(this.xChartAxis);
        }
    }

    getTooltipContent(d) {
        return escape`${d.name}<br>${duration(d.start)} — ${duration(d.stop)}`;
    }
}

export default TimelineView;
