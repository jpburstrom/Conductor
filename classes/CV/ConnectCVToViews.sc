
CVSync {
	classvar <>all;
	classvar <>speedlimDur;
	var <>cv, <>view, speedlim, speedlimRout;

	*initClass {all = IdentityDictionary.new; speedlimDur=0.03; }

	*new { | cv, view | ^super.newCopyArgs(cv, view).init }


	init {
		this.linkToCV;
		this.linkToView;
		speedlim = false;
		speedlimRout = Routine({
			loop {
				speedlimDur.wait;
				if (cv.input != speedlim) {
					this.prUpdateView;
				};
				speedlim = false;
				\hang.yield;
			}
		});
		this.update(cv, \synch);
	}

	linkToCV {
		cv.addDependant(this); 		 	// when CV changes CVsync:update is called
	}

	linkToView {
		view.action = this;
		CVSync.all[view] = CVSync.all[view].add(this);
		view.onClose = CVSync
	}

	update { | changer, what ...moreArgs |	// called when CV changes
		switch( what,
			\synch, { this.updateView ; }
		);
	}

	value { cv.input = view.value }		// called when view changes

	*value { | view | 					// called onClose
		all[view].do(_.remove); all[view] = nil
	}

	remove { cv.removeDependant(this) }

	updateView {
		if (speedlim == false) {
			this.prUpdateView;
			speedlimRout.play;
			speedlim = cv.input;
		}
	}

	prUpdateView {
		defer { view.value = cv.input }
	}


}

CVSyncInput : CVSync {

}

CVSyncValue : CVSync {				// used by NumberBox

	value { cv.value = view.value }

	prUpdateView {
		 defer { view.value = cv.value };
	}
}

CVSyncMulti : CVSync {

	linkToView {
		view.thumbSize = (view.bounds.width - 16 /cv.value.size);
		view.xOffset = 0;
		view.valueThumbSize = 1;
		view.mouseUpAction = this;

		CVSync.all[view] = CVSync.all[view].add(this);
		view.onClose = CVSync;
	}
}

// one view, many CV's.
// CVSyncProperty links one CV to a property of a view
// CVSyncProperties links the view to its CV's

CVSyncProperty : CVSync {
	var <>property;

	*new { | cv, view, property | ^super.newCopyArgs(cv, view, property).init }


	value { cv.input = view.getProperty(property) }

	init {
		this.linkToCV;
		this.update(cv, \synch);
	}

	prUpdateView {
		defer { view.setProperty(property, cv.input) }
	}

}


CVSyncProperties : CVSync {
	var <>links, <>view;

	*new { | cvs, view, properties |
		^super.new(cvs, view)
			.view_(view)
			.links_(properties.collect { | p, i | CVSyncProperty( cvs[i], view, p) })
			.init

	}

 	init {
		this.linkToView;
	}

	value { links.do(_.value) }
	remove { links.do(_.remove) }

}

CVSyncProps {
	var <>props;
	*new { | props | ^super.newCopyArgs(props) }
	new { | cv, view | ^CVSyncProperties(cv, view, props) }
}


SVSync : CVSyncValue {
	init {
		this.update(cv, \items);
		super.init;
	}

	update { | changer, what ...moreArgs |
		switch( what,
			\synch, { this.updateView },
			\items, { defer { view.items = cv.items }; }
		);
	}

	prUpdateView {
		defer  { view.value = cv.value };
	}


}



EVSync : CVSync {

	linkToView {
		view.action = this;
		CVSync.all[view] = CVSync.all[view].add(this);
		view.onClose = CVSync
	}


	value { cv.viewToEV(view) }		// called when view changes


	prUpdateView {
		defer  { cv.evToView(view) }
	}


}

ConductorSync : CVSync {

	linkToCV {
		cv.player.addDependant(this); 		 	// when CV changes CVsync:update is called
	}

	linkToView {
		view.action = this;
		CVSync.all[view] = CVSync.all[view].add(this);
		view.onClose = CVSync;
	}

	value { |m,c,v| if (view.value == 0) { cv.stop } { cv.play } }		// called when view changes

	prUpdateView {
		defer  { view.value = cv.player.value }
	}

}

// use TextFields, TextViews and StaticTexts in CVs
// pre-condition: texts must compile to arrays of numbers

CVSyncText : CVSync {
	classvar <>valRound=0.01;


	value {
		var arr = view.string.interpret;
		if (arr.isKindOf(SequenceableCollection) and:{
			arr.flat.select(_.isNumber).size == arr.flat.size
		}, {
			cv.value = arr.flat;
		})
	}


	prUpdateView {
		defer  { view.string = cv.value.collect(_.round(valRound)).asCompileString };
	}
}

