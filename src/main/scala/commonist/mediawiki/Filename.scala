package commonist.mediawiki

object Filename {
	// NOTE if this is not lazy, andThen is fed with null values
	lazy val fix:String=>String	=
			normalizeSpace	andThen
			undoubleSpace	andThen
			trim			andThen
			dashIllegal		andThen
			ucFirst

	private val normalizeSpace	= (s:String) => s replaceAll ("_", " ")
	private val undoubleSpace	= (s:String) => s replaceAll (" {2,}", " ")
	private val trim			= (s:String) => s.trim
	// see $wgLegalTitleChars and $wgIllegalFileChars
	// """[^ %!\"$&'()*,\-.\/0-9;=?@A-Z\\^_`a-z~\u0080-\u00FF+]"""
	private val dashIllegal		= (s:String) => s replaceAll ("""[\u0000-\u001f\ufffe-\uffff:/<>\[\]\{\}]""", "-")
	private val ucFirst			= (s:String) =>
			if (s.length > 1)	Character.toUpperCase(s charAt 0).toString + (s substring 1)
			else				s

	//------------------------------------------------------------------------------

	/** replaces forbidden characters with '_' */
	def normalizeTitle(title:String):String =
			title
			.map { c =>
				if (c == ' '
					|| c < 32	|| c == 127
					|| c == '<' || c == '>'
					|| c == '[' || c == ']'
					|| c == '{' || c == '}'
					|| c == '|'
		//			|| c == ':'	|| c == '?'
		//			|| c == '/'	|| c == '\\'
		//			|| c == '+'	|| c == '%'
				)	'_'
				else c
			}
			.mkString
}
