export default function Button({
  variant = 'primary',
  className = '',
  ...props
}) {
  const cls = variant === 'ghost' ? 'btn-ghost' : 'btn-primary';
  return <button className={`${cls} ${className}`} {...props} />;
}

