import { MiniCalendar } from "./MiniCalendar";

type Props = {
  date: string;
  onSelectDate: (iso: string) => void;
};

export function LeftSidebar({ date, onSelectDate }: Props) {
  return (
    <aside className="cb-side cb-side--left">
      <div className="cb-profile">
        <div className="cb-profile__avatar" aria-hidden />
        <div className="cb-profile__txt">
          <div className="cb-profile__name">미니가계부</div>
          <div className="cb-profile__sub">Cashbook</div>
        </div>
      </div>

      <MiniCalendar selected={date} onSelect={onSelectDate} />
    </aside>
  );
}
